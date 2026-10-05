[CmdletBinding()]
param(
  [switch]$ForRelease
)
$ErrorActionPreference = 'Continue'

function Test-Tool([string]$Name, [string]$InstallHint) {
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($command) {
    Write-Host "[OK] ${Name}: $($command.Source)" -ForegroundColor Green
    return $true
  }
  Write-Host "[MISSING] $Name - $InstallHint" -ForegroundColor Yellow
  return $false
}

function Test-EnvValue([string]$Name) {
  $value = [Environment]::GetEnvironmentVariable($Name)
  if ($value) {
    Write-Host "[OK] $Name is set" -ForegroundColor Green
    return $true
  }
  Write-Host "[MISSING] $Name is not set in this PowerShell session" -ForegroundColor Yellow
  return $false
}

$ok = $true
Write-Host 'MatchCats preflight check' -ForegroundColor Cyan
Write-Host 'This check is read-only. It does not install software, change files or start services.'
Write-Host ''

$ok = (Test-Tool 'docker' 'Install Docker Desktop, then start it.') -and $ok
$ok = (Test-Tool 'java' 'Install JDK 21 and set JAVA_HOME.') -and $ok
$ok = (Test-Tool 'node' 'Install Node.js LTS for frontend tests and native packaging.') -and $ok
$ok = (Test-Tool 'npm' 'Install Node.js LTS; npm is included.') -and $ok

Write-Host ''
Write-Host 'Application configuration' -ForegroundColor Cyan
foreach ($name in @('DB_URL','DB_USER','DB_PASSWORD')) {
  $ok = (Test-EnvValue $name) -and $ok
}

if ($ForRelease) {
  Write-Host ''
  Write-Host 'Release-only reminders' -ForegroundColor Cyan
  Write-Host '[INFO] Public HTTPS host, production database, backups and monitoring must be selected.'
  foreach ($name in @('APP_CORS_ORIGINS','MAIL_FROM','SMTP_HOST','SMTP_PORT','SMTP_USER','SMTP_PASSWORD')) {
    $ok = (Test-EnvValue $name) -and $ok
  }
  $mailEnabled = [Environment]::GetEnvironmentVariable('MAIL_ENABLED')
  if ($mailEnabled -eq 'true') { Write-Host '[OK] MAIL_ENABLED=true' -ForegroundColor Green }
  else { Write-Host '[MISSING] MAIL_ENABLED must be true for a release' -ForegroundColor Yellow; $ok = $false }
  $cookieSecure = [Environment]::GetEnvironmentVariable('COOKIE_SECURE')
  if ($cookieSecure -eq 'true') { Write-Host '[OK] COOKIE_SECURE=true' -ForegroundColor Green }
  else { Write-Host '[MISSING] COOKIE_SECURE must be true for a release' -ForegroundColor Yellow; $ok = $false }
  $baseUrl = [Environment]::GetEnvironmentVariable('PUBLIC_BASE_URL')
  if ($baseUrl -match '^https://[^/].+') { Write-Host '[OK] PUBLIC_BASE_URL uses HTTPS' -ForegroundColor Green }
  else { Write-Host '[MISSING] PUBLIC_BASE_URL must be an HTTPS URL for a release' -ForegroundColor Yellow; $ok = $false }
  Write-Host '[INFO] SMTP provider, store accounts, signing certificates and privacy/support pages must be configured.'
  Write-Host '[INFO] Premium checkout remains disabled until a payment provider and webhooks are tested.'
  Write-Host '[INFO] This script cannot verify external accounts or credentials.'
}

Write-Host ''
if ($ok) {
  Write-Host 'Preflight passed: required local tools and environment values were found.' -ForegroundColor Green
  exit 0
}
Write-Host 'Preflight found missing local prerequisites. Resolve the marked items, then run it again.' -ForegroundColor Yellow
exit 1
