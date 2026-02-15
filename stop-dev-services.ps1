# stop-dev-services.ps1
# This script stops the development services by terminating their processes.

Write-Host "Stopping Development Services..." -ForegroundColor Cyan

# 1. Stop Java (bootRun)
Write-Host "Stopping Java processes..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*bootRun*" -or $_.CommandLine -like "*thought-to-post-api-service*" } | Stop-Process -Force

# 2. Stop Angular (Node/ng)
Write-Host "Stopping Angular/Node processes..." -ForegroundColor Yellow
Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*ng serve*" -or $_.CommandLine -like "*angular-ui*" } | Stop-Process -Force

# 3. Stop Python AI Agent
Write-Host "Stopping Python processes..." -ForegroundColor Yellow
Get-Process -Name "python" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*src.main*" -or $_.Path -like "*thoughts-to-post-ai-agent*" } | Stop-Process -Force

Write-Host "`nServices stopped." -ForegroundColor Green
