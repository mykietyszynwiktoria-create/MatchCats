$ErrorActionPreference = 'Stop'

$frontend = (Resolve-Path (Join-Path $PSScriptRoot '..\frontend')).Path
$webDir = Join-Path $frontend 'capacitor-web'

if (Test-Path $webDir) {
  Remove-Item -LiteralPath $webDir -Recurse -Force
}
New-Item -ItemType Directory -Path $webDir | Out-Null

$excluded = @('node_modules', 'tests', 'capacitor-web', 'package.json', 'package-lock.json', 'capacitor.config.ts')
Get-ChildItem -LiteralPath $frontend -Force | Where-Object { $excluded -notcontains $_.Name } | ForEach-Object {
  Copy-Item -LiteralPath $_.FullName -Destination $webDir -Recurse -Force
}

Write-Output "Prepared Capacitor web assets in $webDir"
