# start-dev-services.ps1
# This script starts the 3 development services in separate PowerShell windows.

$BaseDir = $PSScriptRoot

Write-Host "Starting Development Services..." -ForegroundColor Cyan

# 1. Run Java API Service
Write-Host "Starting Java API Service..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$BaseDir\thoughts-to-post-api-service'; .\gradlew clean build bootRun"

# 2. Run Angular UI
Write-Host "Starting Angular UI..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$BaseDir\thoughts-to-post-angular-ui'; npm start"

# 3. Run Python AI Agent
Write-Host "Starting Python AI Agent..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$BaseDir\thoughts-to-post-ai-agent'; poetry run python -m src.main"

Write-Host "`nAll service startup commands triggered." -ForegroundColor Green
Write-Host "Check the newly opened windows for build and runtime status." -ForegroundColor Gray
