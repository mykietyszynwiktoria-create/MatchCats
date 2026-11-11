$ErrorActionPreference = 'Stop'
$launcher = Join-Path (Split-Path $PSScriptRoot -Parent) 'Start-MatchCats.ps1'

# The launcher sees these process-local command doubles instead of real services.
function docker { $global:LASTEXITCODE = $global:MatchCatsLauncherTestExitCode }
function java {}

foreach ($case in @(
  @{ ExitCode = 42; Build = $false; ExpectedArguments = $null },
  @{ ExitCode = 0; Build = $false; ExpectedArguments = 'bootRun' },
  @{ ExitCode = 0; Build = $true; ExpectedArguments = 'test build' }
)) {
  $fixture = Join-Path ([IO.Path]::GetTempPath()) ('matchcats-launcher-test-' + [guid]::NewGuid().ToString('N'))
  New-Item -ItemType Directory -Path $fixture | Out-Null
  try {
    [IO.File]::WriteAllText((Join-Path $fixture '.env'), '# isolated test configuration')
    $batch = @('@echo off', 'echo %*>gradle-args.txt', 'exit /b 0') -join [Environment]::NewLine
    [IO.File]::WriteAllText((Join-Path $fixture 'gradlew.bat'), $batch + [Environment]::NewLine, [Text.Encoding]::ASCII)
    $global:MatchCatsLauncherTestExitCode = $case.ExitCode
    $caught = $null
    Push-Location $fixture
    try {
      try { & $launcher -Build:$case.Build }
      catch { $caught = $_ }
    } finally { Pop-Location }

    $argumentsFile = Join-Path $fixture 'gradle-args.txt'
    if ($case.ExitCode -ne 0) {
      if ($null -eq $caught -or $caught.Exception.Message -notlike '*docker compose exit code 42*') {
        throw ('A failed database startup must produce a clear terminating error. Actual: ' + $caught.Exception.Message)
      }
      if (Test-Path -LiteralPath $argumentsFile) { throw 'Gradle started after a failed database startup.' }
    } else {
      if ($null -ne $caught) { throw $caught }
      if (-not (Test-Path -LiteralPath $argumentsFile)) { throw 'Gradle did not start after a successful database startup.' }
      if ((Get-Content -Raw -LiteralPath $argumentsFile).Trim() -ne $case.ExpectedArguments) {
        throw 'The launcher passed unexpected Gradle arguments.'
      }
    }
  } finally {
    # Delete only files created in this test's unique fixture; never recurse.
    foreach ($name in @('.env', 'gradlew.bat', 'gradle-args.txt')) {
      $file = Join-Path $fixture $name
      if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
    }
    Remove-Item -LiteralPath $fixture
  }
}
Write-Output 'PASS: failed Docker startup stops the launcher; successful startup preserves bootRun and -Build behavior.'
