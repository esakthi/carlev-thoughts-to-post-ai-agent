# MongoDB Authentication Troubleshooting Guide

## Problem
You're getting: `Command execution failed on MongoDB server with error 13 (Unauthorized): 'Command update requires authentication'`

## Root Causes & Solutions

### Solution 1: Create MongoDB User (Recommended)

**Windows PowerShell**:
```powershell
# Connect to MongoDB admin database
mongosh --authenticationDatabase admin -u admin -p adminpassword

# Then in the mongosh shell:
use admin
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [
    { role: "readWrite", db: "carlevdb" }
  ]
})

# Verify user was created
db.getUsers()
```

**Or using the batch script**:
```cmd
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
setup-mongodb-auth.bat
```

---

### Solution 2: Run MongoDB Without Authentication (Development Only)

**STEP 1**: Stop MongoDB Service
```powershell
Stop-Service MongoDB
```

**STEP 2**: Find MongoDB Configuration File
- Typical location: `C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg`
- Or: `C:\Program Files\MongoDB\Server\6.0\bin\mongod.cfg`

**STEP 3**: Edit Configuration
Open `mongod.cfg` and find the security section:
```yaml
# Comment out or remove the security section:
# security:
#   authorization: enabled
```

**STEP 4**: Restart MongoDB
```powershell
Start-Service MongoDB
```

**STEP 5**: Update application.properties
```ini
# Remove authSource=admin for no-auth setup
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
```

**STEP 6**: Restart Spring Boot Application

---

### Solution 3: Disable Authentication via MongoDB Command Line

If you have mongosh available:

```bash
# Stop MongoDB
Stop-Service MongoDB

# Start MongoDB without auth flag
mongod --dbpath="C:\data\db"

# In another terminal, verify it's running
mongosh

# Create database
use carlevdb
db.createCollection("topic_events")

# Exit and keep MongoDB running
exit
```

Then update your `application.properties`:
```ini
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
```

---

## Verify Current MongoDB Status

### Check if MongoDB has Authentication Enabled

**Windows PowerShell**:
```powershell
# Check MongoDB service status
Get-Service MongoDB | Select-Object Status

# Check if MongoDB is listening on port 27017
netstat -ano | findstr :27017

# Try to connect
mongosh
```

### Check MongoDB Configuration

```powershell
# Find and display current config
Get-Content "C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg" | Select-String -Pattern "security", "authorization"
```

### View MongoDB Logs

```powershell
# View recent MongoDB logs
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 50

# Search for authentication-related messages
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" | Select-String -Pattern "auth"
```

---

## Quick Diagnosis

Run this PowerShell script to diagnose the issue:

```powershell
Write-Host "MongoDB Diagnosis Report" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check service
Write-Host "[1] MongoDB Service Status:" -ForegroundColor Yellow
$service = Get-Service MongoDB -ErrorAction SilentlyContinue
if ($service) {
    Write-Host "    Status: $($service.Status)" -ForegroundColor Green
} else {
    Write-Host "    MongoDB service not found" -ForegroundColor Red
}

# Test 2: Check port
Write-Host ""
Write-Host "[2] MongoDB Port 27017:" -ForegroundColor Yellow
$port = netstat -ano | findstr :27017
if ($port) {
    Write-Host "    Port is LISTENING (MongoDB running)" -ForegroundColor Green
} else {
    Write-Host "    Port NOT listening (MongoDB not running)" -ForegroundColor Red
}

# Test 3: Connection test
Write-Host ""
Write-Host "[3] MongoDB Connection Test:" -ForegroundColor Yellow
try {
    $result = mongosh --eval "print('Connected')" 2>$null
    if ($result -like "*Connected*") {
        Write-Host "    ✓ Can connect WITHOUT authentication" -ForegroundColor Green
        Write-Host "    This means: MongoDB is running WITHOUT auth enabled" -ForegroundColor Cyan
        Write-Host "    Solution: Use URI without credentials in application.properties" -ForegroundColor Cyan
    }
} catch {
    Write-Host "    ✗ Cannot connect (MongoDB may not be running)" -ForegroundColor Red
}

# Test 4: Verify credentials
Write-Host ""
Write-Host "[4] Authentication Test:" -ForegroundColor Yellow
try {
    $result = mongosh admin --eval "db.auth('carlevuser', 'carlevpassword'); print('Auth OK')" 2>$null
    if ($result -like "*Auth OK*") {
        Write-Host "    ✓ Credentials work: carlevuser / carlevpassword" -ForegroundColor Green
    }
} catch {
    Write-Host "    ✗ Credentials don't work (user may not exist)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=========================" -ForegroundColor Cyan
```

---

## Configuration Examples

### If Using Authentication (Credentials Exist)
```ini
# application.properties
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

### If NOT Using Authentication (No Auth Required)
```ini
# application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
```

### If Using Different Credentials
```ini
# application.properties
spring.data.mongodb.uri=mongodb://YOUR_USERNAME:YOUR_PASSWORD@localhost:27017/carlevdb?authSource=admin
```

---

## Best Practice: Check MongoDB Config File

**Windows Command**:
```cmd
type "C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg" | findstr /i "security authorization"
```

**Output Examples**:

**With Auth Enabled** (you need credentials):
```yaml
security:
  authorization: enabled
```

**Without Auth** (no credentials needed):
```yaml
# security:
#   authorization: enabled
```

---

## Step-by-Step: Get It Working

### Quick Fix (Disable Auth - Dev Only)

1. **Stop MongoDB**
   ```powershell
   Stop-Service MongoDB
   ```

2. **Edit config** - Open `C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg`
   ```yaml
   # Comment out this section:
   # security:
   #   authorization: enabled
   ```

3. **Start MongoDB**
   ```powershell
   Start-Service MongoDB
   ```

4. **Update application.properties**
   ```ini
   spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
   ```

5. **Restart Spring Boot** - Your app should now work!

---

## Verify Fix

After making changes, test with PowerShell:

```powershell
# Test the API endpoint
$response = Invoke-RestMethod -Uri "http://localhost:8081/new/event" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body @{
        title="Test Event"
        description="Testing connection"
        source="Test"
        url="https://test.com"
        category="Test"
        publishedAt=(Get-Date -Format "o")
    } | ConvertTo-Json

Write-Host "Response: $response" -ForegroundColor Green
```

---

## Support Resources

- **MongoDB Manual**: https://docs.mongodb.com/manual/
- **MongoDB Authentication**: https://docs.mongodb.com/manual/core/authentication/
- **MongoDB Connection String**: https://docs.mongodb.com/manual/reference/connection-string/
