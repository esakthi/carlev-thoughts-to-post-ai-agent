# MongoDB Authentication Error - Complete Solution Guide

## Current Issue
```
org.springframework.data.mongodb.UncategorizedMongoDbException: 
Command execution failed on MongoDB server with error 13 (Unauthorized): 
'Command update requires authentication' on server localhost:27017
```

## Why This is Happening

The error suggests one of these scenarios:

1. **MongoDB has authentication enabled BUT the user doesn't exist**
2. **MongoDB has authentication enabled BUT the password is wrong**
3. **MongoDB requires authentication BUT your app isn't providing credentials**
4. **Authentication database mismatch (not using authSource=admin)**

---

## QUICK DIAGNOSIS (RUN THIS FIRST)

### PowerShell
```powershell
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
.\diagnose-mongodb.ps1
```

This will:
- ✓ Check MongoDB service status
- ✓ Check if port 27017 is listening
- ✓ Test connection without credentials
- ✓ Test connection with credentials
- ✓ Show your current configuration
- ✓ Recommend the exact fix needed

---

## IMMEDIATE FIXES (Choose One Based on Your Scenario)

### FIX 1: MongoDB Running WITHOUT Authentication (Simplest)

**Signs**: Can connect to MongoDB without providing any credentials

**Step 1**: Stop MongoDB
```powershell
Stop-Service MongoDB
```

**Step 2**: Edit MongoDB Config
- Open: `C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg`
- Find and comment out the security section:
```yaml
# BEFORE:
security:
  authorization: enabled

# AFTER:
# security:
#   authorization: enabled
```

**Step 3**: Start MongoDB
```powershell
Start-Service MongoDB
```

**Step 4**: Update application.properties
```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration WITHOUT Authentication
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
spring.data.mongodb.auto-index-creation=true

# ... rest of configuration
```

**Step 5**: Restart Spring Boot
```bash
./gradlew clean build
./gradlew bootRun
```

---

### FIX 2: MongoDB WITH Authentication (User Doesn't Exist)

**Signs**: Connection fails with "Unauthorized" error

**Step 1**: Connect to MongoDB WITHOUT authentication
```bash
mongosh
```

**Step 2**: Create the admin user (first time only)
```javascript
use admin
db.createUser({
  user: "admin",
  pwd: "adminpassword",
  roles: ["root"]
})
```

**Step 3**: Create the application user
```javascript
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [
    { role: "readWrite", db: "carlevdb" }
  ]
})
```

**Step 4**: Verify the user was created
```javascript
db.getUsers()
```

**Step 5**: Exit MongoDB shell
```javascript
exit
```

**Step 6**: Verify application.properties has correct URI
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

**Step 7**: Restart Spring Boot
```bash
./gradlew clean build
./gradlew bootRun
```

---

### FIX 3: Wrong Credentials (User Exists But Password Wrong)

**Step 1**: Connect as admin to verify
```bash
mongosh --authenticationDatabase admin -u admin -p adminpassword
```

**Step 2**: Check if carlevuser exists
```javascript
use admin
db.getUsers()
```

**Step 3**: If user exists, drop and recreate it
```javascript
db.dropUser("carlevuser")
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [
    { role: "readWrite", db: "carlevdb" }
  ]
})
```

**Step 4**: Update application.properties
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

**Step 5**: Restart Spring Boot

---

## FULL SETUP SCRIPT (Automated)

If you want to automate the setup:

### Windows Batch
```cmd
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
setup-mongodb-auth.bat
```

---

## TESTING THE FIX

### After making changes, verify with PowerShell:

```powershell
# Make sure Spring Boot is running on 8081

# Test 1: Simple event creation
$body = @{
    title = "Test Event"
    description = "Testing MongoDB fix"
    source = "Test"
    url = "https://test.com"
    category = "Testing"
    publishedAt = (Get-Date -Format "o")
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8081/api/v1/events/new/event" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

Write-Host "Response:" -ForegroundColor Green
$response | ConvertTo-Json | Write-Host

# Expected output: HTTP 201 Created with event details
```

---

## TROUBLESHOOTING IF STILL NOT WORKING

### Check MongoDB Logs
```powershell
# View latest MongoDB logs
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 50

# Search for authentication errors
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" | Select-String "auth|Auth|UNAUTHORIZED"
```

### Check Spring Boot Logs
Look for messages like:
- "MongoTemplate initialized" - ✓ Good
- "Unauthorized" - ✗ Credentials wrong
- "Connection refused" - ✗ MongoDB not running

### Verify MongoDB Service
```powershell
# Check status
Get-Service MongoDB | Select-Object Status

# Restart service
Restart-Service MongoDB

# Check if listening on port 27017
netstat -ano | findstr :27017
```

### Test MongoDB Connection Directly
```bash
# Without credentials
mongosh

# With credentials
mongosh --authenticationDatabase admin -u carlevuser -p carlevpassword

# Full connection test
mongosh "mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin"
```

---

## REFERENCE: Configuration Options

| Scenario | Connection String |
|----------|-------------------|
| No authentication | `mongodb://localhost:27017/carlevdb` |
| With authentication | `mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin` |
| Different database | `mongodb://user:pass@localhost:27017/otherdb?authSource=admin` |
| Different user | `mongodb://otheruser:otherpass@localhost:27017/carlevdb?authSource=admin` |

---

## DECISION TREE

```
Is MongoDB running?
├─ NO: Start MongoDB (Start-Service MongoDB)
├─ YES: Can you connect without credentials (mongosh)?
   ├─ YES: Use URI without credentials
   │   └─ spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
   ├─ NO: Does user 'carlevuser' exist (mongosh --help)?
      ├─ NO: Create user using MongoDB shell
      │   └─ db.createUser({user:"carlevuser", pwd:"carlevpassword", roles:[{role:"readWrite", db:"carlevdb"}]})
      ├─ YES: Is password correct?
         ├─ NO: Drop and recreate user
         ├─ YES: Use URI with credentials
            └─ spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

---

## FILES IN THIS DIRECTORY

| File | Purpose |
|------|---------|
| `diagnose-mongodb.ps1` | Run this first to diagnose the issue |
| `setup-mongodb-auth.bat` | Automated setup script |
| `MONGODB_TROUBLESHOOTING.md` | Detailed troubleshooting guide |
| `application.properties` | Your Spring Boot configuration |

---

## NEXT STEPS

1. **Run diagnosis**: `.\diagnose-mongodb.ps1`
2. **Follow the recommended fix** based on output
3. **Update application.properties** if needed
4. **Restart Spring Boot** application
5. **Test the API** with sample JSON request
6. **Check logs** if still having issues

---

## Need More Help?

Check these resources:
- MongoDB Documentation: https://docs.mongodb.com/manual/
- MongoDB Authentication: https://docs.mongodb.com/manual/core/authentication/
- Spring Data MongoDB: https://spring.io/projects/spring-data-mongodb
