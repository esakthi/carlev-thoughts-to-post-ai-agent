# Copy-Paste Commands - MongoDB Authentication Fix

## Use This File to Copy Exact Commands

Just copy the command from below and paste into your terminal.

---

## STEP 1: Navigate to Project
```powershell
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
```

---

## STEP 2: Run Diagnostic (REQUIRED - Start Here)
```powershell
.\diagnose-mongodb.ps1
```

**Read the output carefully. It will tell you if you need Fix A or Fix B.**

---

## STEP 3A: If Diagnostic Says "Can connect without credentials"

### Update application.properties

```powershell
# Open the file in Notepad
notepad src\main\resources\application.properties
```

**Replace the MongoDB section with:**
```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - NO AUTHENTICATION
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
spring.data.mongodb.auto-index-creation=true

# Kafka Producer Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.batch-size=16384
spring.kafka.producer.linger-ms=10
spring.kafka.producer.buffer-memory=33554432
spring.kafka.producer.enable-idempotence=true
spring.kafka.producer.max-in-flight-requests-per-connection=5
spring.kafka.producer.compression-type=snappy

# Kafka Topic Configuration
kafka.topic.events=topic-events
```

**Save and close.**

Then skip to STEP 4.

---

## STEP 3B: If Diagnostic Says "Credentials failed" or "User doesn't exist"

### Create MongoDB User

```bash
mongosh
```

**In the MongoDB shell, copy-paste this:**
```javascript
use admin
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [{ role: "readWrite", db: "carlevdb" }]
})
exit
```

**Then verify application.properties has this URI:**
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

If not, update it and save.

---

## STEP 4: Clean Build

```bash
./gradlew clean build
```

**Wait for it to complete.**

---

## STEP 5: Run Application

```bash
./gradlew bootRun
```

**Keep this window open. You should see:**
```
2026-01-16T... INFO ... Started application
```

---

## STEP 6: Test in NEW PowerShell Window

**Open a NEW PowerShell window and paste:**

```powershell
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
```

---

## EXPECTED SUCCESS

You should see output like:
```
Response:
{
  "id": "evt-xxxxx",
  "title": "Test Event",
  "message": "Event processed successfully and sent to Kafka",
  "status": "COLLECTED"
}
```

**If you see this, your fix worked!** ✅

---

## If You Get an Error

### Check 1: Is MongoDB Running?
```powershell
Get-Service MongoDB | Select-Object Status
```

Should show: `Status: Running`

If not, start it:
```powershell
Start-Service MongoDB
```

### Check 2: Is Port Listening?
```powershell
netstat -ano | findstr :27017
```

Should show a line with 27017. If not, MongoDB isn't running.

### Check 3: View Application Logs
Look at the terminal where you ran `./gradlew bootRun`

Search for error messages. Common ones:
- "Unauthorized" → User doesn't exist (go back to Step 3B)
- "Connection refused" → MongoDB not running (restart it)
- "Database not found" → This is OK, will be created

### Check 4: View MongoDB Logs
```powershell
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 50
```

Look for any error messages.

---

## Quick Commands Reference

### Check MongoDB Status
```powershell
Get-Service MongoDB | Select-Object Status
```

### Start MongoDB
```powershell
Start-Service MongoDB
```

### Stop MongoDB
```powershell
Stop-Service MongoDB
```

### Connect to MongoDB with Credentials
```bash
mongosh -u carlevuser -p carlevpassword --authenticationDatabase admin
```

### Connect to MongoDB without Credentials
```bash
mongosh
```

### View MongoDB Users
```bash
mongosh
use admin
db.getUsers()
exit
```

### Drop a User (if needed to recreate)
```bash
mongosh -u admin -p adminpassword
use admin
db.dropUser("carlevuser")
exit
```

### Create User Again
```bash
mongosh
use admin
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [{ role: "readWrite", db: "carlevdb" }]
})
exit
```

### Kill Process on Port 8081 (if app won't start)
```powershell
Get-Process | Where-Object { $_.ProcessName -like "*java*" } | Stop-Process -Force
```

### Clean Everything and Restart
```bash
Stop-Service MongoDB
./gradlew clean
Start-Service MongoDB
./gradlew bootRun
```

---

## Complete One-Command Fix (If You Know What You're Doing)

**For No Authentication:**
```bash
notepad src\main\resources\application.properties
# Change URI to: spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
# Save
./gradlew clean build && ./gradlew bootRun
```

**For With Authentication (User Already Exists):**
```bash
./gradlew clean build && ./gradlew bootRun
```

**For With Authentication (User Doesn't Exist):**
```bash
mongosh
# Paste in MongoDB shell:
use admin; db.createUser({user:"carlevuser",pwd:"carlevpassword",roles:[{role:"readWrite",db:"carlevdb"}]})
exit
# Then:
./gradlew clean build && ./gradlew bootRun
```

---

## Troubleshooting Copy-Paste Commands

### If You Get "PowerShell Script Disabled" Error
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Then try again:
```powershell
.\diagnose-mongodb.ps1
```

### If Gradle Fails
Make sure you're in the correct directory:
```powershell
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
pwd  # Should show the project path
ls   # Should show build.gradle, settings.gradle, etc.
```

### If mongosh Command Not Found
MongoDB CLI tools might not be in PATH. Try:
```bash
"C:\Program Files\MongoDB\Tools\mongosh.exe"
```

Or download MongoDB Shell from:
https://www.mongodb.com/try/download/shell

---

## Still Stuck?

1. Copy the diagnostic output:
   ```powershell
   .\diagnose-mongodb.ps1 | Out-File diagnose-output.txt
   ```

2. Check the output file

3. Read `SOLUTION_GUIDE.md` with the diagnostic output

4. If still stuck, check MongoDB logs:
   ```powershell
   Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 100 | Out-File mongod-logs.txt
   ```

5. Read the logs file and search for error patterns

---

## Command Cheat Sheet

| Task | Command |
|------|---------|
| Navigate | `cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service` |
| Diagnose | `.\diagnose-mongodb.ps1` |
| Edit config | `notepad src\main\resources\application.properties` |
| Build | `./gradlew clean build` |
| Run | `./gradlew bootRun` |
| Test | `Invoke-RestMethod -Uri "http://localhost:8081/api/v1/events/new/event" ...` |
| Check MongoDB | `Get-Service MongoDB` |
| Connect MongoDB | `mongosh` |
| View logs | `Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 50` |

---

## Final Checklist Before Running

- [ ] PowerShell is open
- [ ] I'm in the project directory
- [ ] MongoDB is installed
- [ ] I've run the diagnostic: `.\diagnose-mongodb.ps1`
- [ ] I've read the diagnostic output
- [ ] I understand whether to use Fix A or Fix B
- [ ] I'm ready to run the commands

**If all checked ✅ → Go ahead and start with Step 1 above**
