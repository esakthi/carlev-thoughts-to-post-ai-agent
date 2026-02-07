# MongoDB Diagnostic Script
# This script will help identify the exact MongoDB authentication issue

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MongoDB Diagnostic Report" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: MongoDB Service
Write-Host "[TEST 1] MongoDB Service Status" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow
$service = Get-Service MongoDB -ErrorAction SilentlyContinue
if ($service) {
    $status = $service.Status
    Write-Host "Service found: MongoDB"
    Write-Host "Status: $status" -ForegroundColor $(if ($status -eq "Running") { "Green" } else { "Red" })
} else {
    Write-Host "MongoDB service not found as Windows service" -ForegroundColor Yellow
    Write-Host "This might be OK if MongoDB is running manually" -ForegroundColor Cyan
}
Write-Host ""

# Test 2: Port Listening
Write-Host "[TEST 2] MongoDB Port 27017" -ForegroundColor Yellow
Write-Host "----------------------------" -ForegroundColor Yellow
$port = netstat -ano | findstr :27017
if ($port) {
    Write-Host "✓ Port 27017 is LISTENING" -ForegroundColor Green
    Write-Host "MongoDB is running and accessible" -ForegroundColor Green
} else {
    Write-Host "✗ Port 27017 is NOT listening" -ForegroundColor Red
    Write-Host "MongoDB is not running or not accessible" -ForegroundColor Red
    Write-Host ""
    Write-Host "Fix: Start MongoDB" -ForegroundColor Yellow
    Write-Host "  - Windows Service: Start-Service MongoDB" -ForegroundColor Cyan
    Write-Host "  - Or manually: mongod" -ForegroundColor Cyan
}
Write-Host ""

# Test 3: Connection Without Auth
Write-Host "[TEST 3] Connection Test (No Auth)" -ForegroundColor Yellow
Write-Host "-----------------------------------" -ForegroundColor Yellow
try {
    $result = mongosh --eval "db.getName()" 2>&1
    if ($result -notlike "*error*" -and $result -notlike "*Error*") {
        Write-Host "✓ Can connect WITHOUT authentication" -ForegroundColor Green
        Write-Host ""
        Write-Host "DIAGNOSIS: MongoDB is running WITHOUT authentication" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "SOLUTION:" -ForegroundColor Yellow
        Write-Host "  1. Update application.properties:" -ForegroundColor Cyan
        Write-Host "     spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb" -ForegroundColor Green
        Write-Host ""
        Write-Host "  2. Restart Spring Boot application" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "Cannot connect without credentials" -ForegroundColor Yellow
        Write-Host "MongoDB might have authentication enabled" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Connection failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Check Credentials
Write-Host "[TEST 4] Credentials Test" -ForegroundColor Yellow
Write-Host "-------------------------" -ForegroundColor Yellow
try {
    $result = mongosh --authenticationDatabase admin --username carlevuser --password carlevpassword --eval "print('Auth OK')" 2>&1
    if ($result -like "*Auth OK*") {
        Write-Host "✓ Credentials WORK: carlevuser / carlevpassword" -ForegroundColor Green
        Write-Host ""
        Write-Host "DIAGNOSIS: Authentication configured and working" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "NEXT STEPS:" -ForegroundColor Yellow
        Write-Host "  1. Verify application.properties has:" -ForegroundColor Cyan
        Write-Host "     spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin" -ForegroundColor Green
        Write-Host ""
        Write-Host "  2. Check MongoDB logs for connection issues" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "✗ Credentials FAILED" -ForegroundColor Red
        Write-Host "User 'carlevuser' doesn't exist or password is wrong" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Could not test credentials: $_" -ForegroundColor Yellow
}
Write-Host ""

# Test 5: MongoDB Config File
Write-Host "[TEST 5] MongoDB Configuration" -ForegroundColor Yellow
Write-Host "------------------------------" -ForegroundColor Yellow
$configPath = "C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg"
$configPath2 = "C:\Program Files\MongoDB\Server\6.0\bin\mongod.cfg"
$configPath3 = "C:\Program Files\MongoDB\Server\5.0\bin\mongod.cfg"

$actualPath = $null
if (Test-Path $configPath) { $actualPath = $configPath }
elseif (Test-Path $configPath2) { $actualPath = $configPath2 }
elseif (Test-Path $configPath3) { $actualPath = $configPath3 }

if ($actualPath) {
    Write-Host "Config file found: $actualPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "Security settings:" -ForegroundColor Cyan
    $config = Get-Content $actualPath
    $security = $config | Select-String -Pattern "security|authorization" -Context 0,2
    if ($security) {
        Write-Host $security
    } else {
        Write-Host "No security settings found (authentication likely disabled)" -ForegroundColor Cyan
    }
} else {
    Write-Host "MongoDB config file not found at standard locations" -ForegroundColor Yellow
}
Write-Host ""

# Test 6: Current application.properties
Write-Host "[TEST 6] Current application.properties" -ForegroundColor Yellow
Write-Host "--------------------------------------" -ForegroundColor Yellow
$appPropsPath = ".\src\main\resources\application.properties"
if (Test-Path $appPropsPath) {
    Write-Host "Current MongoDB URI:" -ForegroundColor Cyan
    Get-Content $appPropsPath | Select-String "mongodb" | ForEach-Object {
        Write-Host "  $_" -ForegroundColor Green
    }
} else {
    Write-Host "Could not find application.properties" -ForegroundColor Red
}
Write-Host ""

# Final Recommendation
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RECOMMENDED ACTION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Based on the tests above, choose the appropriate solution:" -ForegroundColor Yellow
Write-Host ""
Write-Host "SCENARIO 1: MongoDB running WITHOUT authentication" -ForegroundColor Cyan
Write-Host "  └─ Update application.properties to:" -ForegroundColor White
Write-Host "     spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb" -ForegroundColor Green
Write-Host ""
Write-Host "SCENARIO 2: MongoDB running WITH authentication (user exists)" -ForegroundColor Cyan
Write-Host "  └─ Keep or update application.properties to:" -ForegroundColor White
Write-Host "     spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin" -ForegroundColor Green
Write-Host ""
Write-Host "SCENARIO 3: MongoDB running WITH authentication (user DOESN'T exist)" -ForegroundColor Cyan
Write-Host "  └─ Create the user using mongosh:" -ForegroundColor White
Write-Host "     mongosh admin" -ForegroundColor Gray
Write-Host "     db.createUser({" -ForegroundColor Gray
Write-Host "       user: 'carlevuser'," -ForegroundColor Gray
Write-Host "       pwd: 'carlevpassword'," -ForegroundColor Gray
Write-Host "       roles: [{ role: 'readWrite', db: 'carlevdb' }]" -ForegroundColor Gray
Write-Host "     })" -ForegroundColor Gray
Write-Host ""
Write-Host "  └─ Then update application.properties to:" -ForegroundColor White
Write-Host "     spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin" -ForegroundColor Green
Write-Host ""
Write-Host "SCENARIO 4: MongoDB NOT running" -ForegroundColor Cyan
Write-Host "  └─ Start MongoDB:" -ForegroundColor White
Write-Host "     Start-Service MongoDB" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "After making changes, restart your Spring Boot application:" -ForegroundColor Yellow
Write-Host "  ./gradlew bootRun" -ForegroundColor Green
Write-Host ""
