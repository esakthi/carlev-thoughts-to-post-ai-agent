# ✅ MongoDB Fix Execution Checklist

## Before You Start
- [ ] PowerShell is open
- [ ] MongoDB is installed on this machine
- [ ] You're in the project directory: `C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service`
- [ ] You have all 11+ files in your project folder

**Ready?** → Continue below

---

## PHASE 1: Diagnosis (5 minutes)

### Step 1.1: Navigate to Project
```powershell
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service
```
- [ ] Command executed
- [ ] Directory changed (verify with: `pwd`)

### Step 1.2: Run Diagnostic Script
```powershell
.\diagnose-mongodb.ps1
```
- [ ] Script started
- [ ] Script completed
- [ ] Output is visible

### Step 1.3: Read Diagnostic Output
- [ ] MongoDB service status: **____________**
- [ ] Port 27017 listening: **YES / NO**
- [ ] Can connect without credentials: **YES / NO**
- [ ] Credentials test result: **PASS / FAIL**
- [ ] **Recommended Fix**: **A / B / UNKNOWN**

### Step 1.4: Determine Your Scenario
- [ ] Scenario A: "Can connect without credentials" → Fix A
- [ ] Scenario B: "Cannot connect / Credentials failed" → Fix B
- [ ] Scenario Other: See SOLUTION_GUIDE.md

---

## PHASE 2: Apply Fix (10-15 minutes)

### If Your Scenario is A (No Authentication)

#### Step 2A.1: Edit Configuration File
```powershell
notepad src\main\resources\application.properties
```
- [ ] File opened in Notepad

#### Step 2A.2: Find MongoDB Configuration Section
Look for:
```ini
spring.data.mongodb.uri=...
```
- [ ] Section found

#### Step 2A.3: Replace With
```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - NO AUTHENTICATION
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
spring.data.mongodb.auto-index-creation=true

# Kafka Producer Configuration
spring.kafka.bootstrap-servers=localhost:9092
...
```
- [ ] Replaced correctly
- [ ] Removed authSource parameter
- [ ] Removed username:password

#### Step 2A.4: Save and Close
- [ ] File saved (Ctrl+S)
- [ ] Notepad closed

#### Skip to PHASE 3

---

### If Your Scenario is B (With Authentication)

#### Step 2B.1: Open MongoDB Shell
```bash
mongosh
```
- [ ] MongoDB shell opened
- [ ] Command prompt shows `>` or `test>`

#### Step 2B.2: Create Admin User (First Time Only)
```javascript
use admin
db.createUser({
  user: "admin",
  pwd: "adminpassword",
  roles: ["root"]
})
```
- [ ] Command executed
- [ ] Response shows success (or "user already exists")

#### Step 2B.3: Create Application User
```javascript
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [{ role: "readWrite", db: "carlevdb" }]
})
```
- [ ] Command executed
- [ ] Response shows success (or "user already exists")

#### Step 2B.4: Verify Users Created
```javascript
db.getUsers()
exit
```
- [ ] User list displayed
- [ ] carlevuser is in the list
- [ ] MongoDB shell closed

#### Step 2B.5: Verify Configuration File
```powershell
notepad src\main\resources\application.properties
```
- [ ] File opened

Check that it contains:
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```
- [ ] Connection string is correct
- [ ] Has ?authSource=admin
- [ ] Has username:password
- [ ] File closed

---

## PHASE 3: Rebuild Application (3 minutes)

### Step 3.1: Clean Build
```bash
./gradlew clean build
```
- [ ] Build started
- [ ] Compilation messages appear
- [ ] Build completed successfully (look for "BUILD SUCCESSFUL")

### Step 3.2: Verify Build Success
- [ ] No error messages
- [ ] Last line shows "BUILD SUCCESSFUL"
- [ ] JAR file created: `build/libs/carlev-api-service-0.0.1-SNAPSHOT.jar`

---

## PHASE 4: Start Application (Immediate)

### Step 4.1: Run Application
```bash
./gradlew bootRun
```
- [ ] Application starting...
- [ ] Messages appearing

### Step 4.2: Wait for Startup
Look for:
```
Started application in X seconds
```
- [ ] Application is running
- [ ] No MongoDB errors visible
- [ ] No "Unauthorized" errors
- [ ] Port 8081 is listening

**KEEP THIS WINDOW OPEN** - Don't close it

---

## PHASE 5: Test in New Window (2 minutes)

### Step 5.1: Open NEW PowerShell Window
- [ ] New PowerShell window opened
- [ ] Original Spring Boot window still shows output

### Step 5.2: Run Test Script
```powershell
./test-api.ps1
```
- [ ] Script started
- [ ] Test 1 running...
- [ ] Test 2 running...
- [ ] Test 3 running...
- [ ] All tests completed

### Step 5.3: Check Test Results
Look for responses like:
```json
{
  "id": "...",
  "message": "Event processed successfully",
  "status": "COLLECTED"
}
```
- [ ] HTTP 201 response received
- [ ] No error messages
- [ ] Event ID returned
- [ ] Status shows "COLLECTED" or "PROCESSED"

---

## PHASE 6: Verify in MongoDB (Optional)

### Step 6.1: Connect to MongoDB
```bash
mongosh
```
- [ ] Connected to MongoDB

### Step 6.2: Check Database
```javascript
use carlevdb
db.topic_events.find()
exit
```
- [ ] Collection listed
- [ ] Events visible in output
- [ ] MongoDB shell closed

---

## Success Checklist

### Application Status
- [ ] Spring Boot running without errors
- [ ] No "Unauthorized" errors in logs
- [ ] No "Connection refused" errors
- [ ] Application accessible on port 8081

### API Testing
- [ ] test-api.ps1 ran successfully
- [ ] Got HTTP 201 responses
- [ ] Event IDs returned
- [ ] No error messages

### Data Verification
- [ ] Events saved to MongoDB
- [ ] Can retrieve events from MongoDB
- [ ] No authentication errors

---

## Verification Checklist

After all phases complete, verify:

### ✅ Minimum (Application Working)
- [ ] Spring Boot started successfully
- [ ] No MongoDB auth errors
- [ ] Got HTTP 201 response from API
- [ ] Application is responding

### ✅ Complete (Everything Working)
- [ ] All tests in test-api.ps1 passed
- [ ] Events visible in MongoDB
- [ ] No errors in Spring Boot logs
- [ ] API response time is fast

---

## Troubleshooting Quick Links

If something failed:

| Error | Check | Fix |
|-------|-------|-----|
| "Connection refused" | MongoDB running | Start: `Start-Service MongoDB` |
| "Unauthorized" | Credentials correct | Check password, recreate user |
| "Build failed" | Java/Gradle version | Check: `./gradlew --version` |
| "Port 8081 in use" | Other process | Kill: `netstat -ano \| findstr :8081` |
| "Cannot find script" | File location | Verify: `dir diagnose-mongodb.ps1` |

---

## What to Do Now

### ✅ You're Ready
If all "Before You Start" boxes are checked:
1. Continue to PHASE 1
2. Follow each step
3. Check the boxes as you complete them

### ⚠️ You're Not Ready
If anything is missing:
1. Install MongoDB (if missing)
2. Navigate to correct directory
3. Verify files exist
4. Then continue

---

## Time Breakdown

| Phase | Time | Status |
|-------|------|--------|
| Diagnosis | 5 min | ⏳ |
| Apply Fix | 10 min | ⏳ |
| Rebuild | 3 min | ⏳ |
| Start App | 1 min | ⏳ |
| Test | 2 min | ⏳ |
| **Total** | **21 min** | ⏳ |

**Estimated completion**: ~21 minutes from now

---

## How to Track Progress

As you complete each section:
1. Check the box: `- [x]`
2. Update status: `✅`
3. Note any issues
4. Continue to next section

---

## Common Issues During Execution

### Issue: PowerShell says "Script Disabled"
**Solution**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Issue: MongoDB Shell Not Found
**Solution**:
```bash
# Use full path
"C:\Program Files\MongoDB\Tools\mongosh.exe"
```

### Issue: Gradle Build Fails
**Solution**:
```bash
# Clean and retry
./gradlew clean
./gradlew build
```

### Issue: Port 8081 Already in Use
**Solution**:
```powershell
# Find and kill process
Get-Process | Where-Object { $_.ProcessName -like "*java*" } | Stop-Process -Force
```

---

## After Completion

### ✅ If Everything Works
- [ ] Application is stable
- [ ] Tests pass
- [ ] Data saves correctly
- [ ] You can proceed with development

### ⚠️ If Something Still Fails
- [ ] Check MONGODB_TROUBLESHOOTING.md
- [ ] Review diagnostic output
- [ ] Check Spring Boot logs for details
- [ ] Verify MongoDB configuration

---

## Quick Status Update

At any point, you can verify status:

**Is MongoDB running?**
```powershell
Get-Service MongoDB | Select-Object Status
```

**Is application running?**
```powershell
# Check if port 8081 is open
netstat -ano | findstr :8081
```

**Can you connect to API?**
```powershell
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/events/health"
```

---

## Final Notes

- ✅ This checklist covers all steps
- ✅ Check boxes as you progress
- ✅ Note any errors encountered
- ✅ Keep this document for reference
- ✅ You have 21 minutes total

---

## You're All Set! 🚀

- [x] Checklist reviewed
- [x] Ready to start
- [x] Have all files
- [x] Understand process

**Next**: Go to PHASE 1 and start checking boxes!
