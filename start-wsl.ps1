# Start-WSL.ps1
# Use this to start the platform from Windows PowerShell

Write-Host "Triggering WSL Startup Script..." -ForegroundColor Cyan

# This executes the bash script inside your default WSL distro (Ubuntu)
wsl bash -c "./start-kafka-wsl-and-thoughts2-post.sh"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nPlatform is starting up!" -ForegroundColor Green
    Write-Host "You can close this window; the services are running in the background/WSL." -ForegroundColor Gray
} else {
    Write-Host "`nError starting services. check if WSL is running." -ForegroundColor Red
}
