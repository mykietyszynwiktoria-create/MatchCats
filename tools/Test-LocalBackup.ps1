param(
    [Parameter(Mandatory=$true)][string]$WorkspaceDirectory,
    [Parameter(Mandatory=$true)][string]$BackupDirectory,
    [Parameter(Mandatory=$true)][string]$ScratchDirectory,
    [Parameter(Mandatory=$true)][string]$PostgreSQLBin,
    [Parameter(Mandatory=$true)][string]$JavaExecutable,
    [Parameter(Mandatory=$true)][string]$JdbcJar,
    [int]$DatabasePort=55433,
    [int]$ApplicationPort=8086
)
$ErrorActionPreference='Stop'
$taskWorkspace=(Resolve-Path -LiteralPath $WorkspaceDirectory).Path.TrimEnd('\')
$taskBackup=(Resolve-Path -LiteralPath $BackupDirectory).Path
$taskScratch=[IO.Path]::GetFullPath($ScratchDirectory)
if(-not $taskBackup.StartsWith($taskWorkspace+'\',[StringComparison]::OrdinalIgnoreCase) -or
   -not $taskScratch.StartsWith($taskWorkspace+'\',[StringComparison]::OrdinalIgnoreCase) -or
   $taskScratch.StartsWith($taskBackup+'\',[StringComparison]::OrdinalIgnoreCase) -or
   $taskBackup.StartsWith($taskScratch+'\',[StringComparison]::OrdinalIgnoreCase) -or
   (Test-Path -LiteralPath $taskScratch)){throw 'Use a new scratch directory inside the workspace, separate from the backup'}
foreach($taskPath in @($taskBackup,$taskScratch)){
    $taskAncestor=$taskPath
    while($taskAncestor.StartsWith($taskWorkspace+'\',[StringComparison]::OrdinalIgnoreCase)){
        if((Test-Path -LiteralPath $taskAncestor) -and
           (((Get-Item -LiteralPath $taskAncestor).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)){
            throw 'Linked backup or scratch path is not supported'
        }
        $taskAncestor=[IO.Path]::GetDirectoryName($taskAncestor)
    }
}
$taskSource=Join-Path $taskBackup 'pgdata'
$taskJar=Join-Path $taskBackup 'MatchCats-server.jar'
$taskControl=Join-Path $PostgreSQLBin 'pg_ctl.exe'
if((Get-Content -LiteralPath (Join-Path $taskSource 'PG_VERSION')).Trim() -ne '16'){throw 'This check expects PostgreSQL 16'}
if(Test-Path -LiteralPath (Join-Path $taskSource 'postmaster.pid')){throw 'Backup must be a clean stopped cluster copy'}
if(Get-ChildItem -LiteralPath $taskSource -Recurse -Attributes ReparsePoint){throw 'This local check does not support linked cluster files or tablespaces'}
foreach($taskConfig in @('postgresql.conf','postgresql.auto.conf')){
    $taskConfigPath=Join-Path $taskSource $taskConfig
    if((Test-Path -LiteralPath $taskConfigPath) -and
       (Select-String -LiteralPath $taskConfigPath -Pattern '^\s*(?:(?:data_directory|hba_file|ident_file|external_pid_file)\s*=|include(?:_dir|_if_exists)?(?:\s|=))')){
        throw 'This check requires self-contained cluster configuration without external paths or includes'
    }
}
foreach($taskFile in @($taskJar,$taskControl,$JavaExecutable,$JdbcJar)){
    if(-not (Test-Path -LiteralPath $taskFile -PathType Leaf)){throw "Missing required file: $taskFile"}
}
foreach($taskPort in @($DatabasePort,$ApplicationPort)){
    if($taskPort -notin 1024..65535){throw 'Use a nonprivileged TCP port'}
    $taskListener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$taskPort)
    try{$taskListener.Start()}finally{$taskListener.Stop()}
}
if($DatabasePort -eq $ApplicationPort){throw 'Database and application ports must differ'}
New-Item -ItemType Directory -Path $taskScratch | Out-Null
$taskData=Join-Path $taskScratch 'pgdata'
$taskJava=$null;$taskDatabaseStarted=$false
$taskEnvNames=@('DB_URL','DB_USER','DB_PASSWORD','MAIL_ENABLED','SERVER_ADDRESS','MODERATOR_ACCOUNT_IDS')
$taskOriginalEnv=@{}
foreach($taskName in $taskEnvNames){$taskOriginalEnv[$taskName]=[Environment]::GetEnvironmentVariable($taskName,'Process')}
try {
    Copy-Item -LiteralPath $taskSource -Destination $taskData -Recurse
    $taskDatabaseStarted=$true
    & $taskControl start -D $taskData -o "-h 127.0.0.1 -p $DatabasePort" -l (Join-Path $taskScratch 'postgres.log') -w
    if($LASTEXITCODE -ne 0){throw 'Restored database did not start'}
    & $JavaExecutable --class-path $JdbcJar (Join-Path $PSScriptRoot 'BackupRestoreCheck.java') "jdbc:postgresql://127.0.0.1:$DatabasePort/matchcats_dev"
    if($LASTEXITCODE -ne 0){throw 'Restored database checks failed'}
    $env:DB_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/matchcats_dev"
    $env:DB_USER='postgres';$env:DB_PASSWORD='';$env:MAIL_ENABLED='false'
    $env:SERVER_ADDRESS='127.0.0.1';$env:MODERATOR_ACCOUNT_IDS=''
    $taskJava=Start-Process -FilePath $JavaExecutable -WindowStyle Hidden -PassThru `
        -ArgumentList @('-Xms64m','-Xmx256m','-XX:MaxMetaspaceSize=192m','-jar',('"'+$taskJar+'"'),"--server.port=$ApplicationPort") `
        -RedirectStandardOutput (Join-Path $taskScratch 'application.log') `
        -RedirectStandardError (Join-Path $taskScratch 'application-errors.log')
    $taskReady=$false
    for($taskAttempt=0;$taskAttempt -lt 60;$taskAttempt++){
        $taskJava.Refresh();if($taskJava.HasExited){throw 'Backup application exited; inspect scratch logs'}
        try{
            $taskHealth=Invoke-RestMethod "http://127.0.0.1:$ApplicationPort/health" -TimeoutSec 2
            $taskPage=Invoke-WebRequest "http://127.0.0.1:$ApplicationPort/" -UseBasicParsing -TimeoutSec 2
            if($taskHealth.status -eq 'UP' -and $taskPage.Content.Contains('MatchCats')){$taskReady=$true;break}
        }catch{}
        Start-Sleep -Seconds 1
    }
    if(-not $taskReady){throw 'Backup application did not become ready'}
    Write-Output 'PASS: paired backup database and application started in isolation'
}finally{
    if($null -ne $taskJava){$taskJava.Refresh();if(-not $taskJava.HasExited){Stop-Process -Id $taskJava.Id -Force}}
    if($taskDatabaseStarted -and (Test-Path -LiteralPath (Join-Path $taskData 'postmaster.pid'))){
        & $taskControl -D $taskData stop -m fast -w
        if($LASTEXITCODE -ne 0){Write-Warning 'Could not stop the isolated restored database; inspect scratch directory'}
    }
    foreach($taskName in $taskEnvNames){[Environment]::SetEnvironmentVariable($taskName,$taskOriginalEnv[$taskName],'Process')}
}
