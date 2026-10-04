# MatchCats local development launcher

[CmdletBinding()]
param([switch]$Build)
$ErrorActionPreference = 'Stop'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker Desktop is required. Install it before starting the development database.' }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java 21 is required. Install a JDK and set JAVA_HOME before starting MatchCats.' }
if (-not (Test-Path '.env')) {
  Copy-Item '.env.example' '.env'
  Write-Host 'Created .env from .env.example. Review the local database password before continuing.'
}

docker compose up -d postgres
Write-Host 'PostgreSQL is starting on localhost:5432.'
Write-Host 'Set DB_URL, DB_USER and DB_PASSWORD in this PowerShell session to match .env.'
if ($Build) {
  .\gradlew.bat test build
} else {
  .\gradlew.bat bootRun
}
