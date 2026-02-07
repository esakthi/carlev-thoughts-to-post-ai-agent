# MongoDB Authentication - Quick Reference Card

## The Problem
```
Error 13 (Unauthorized): 'Command update requires authentication'
```

## The Solution Flow

```
┌─────────────────────────────────────┐
│  Run: .\diagnose-mongodb.ps1        │
└────────────────┬────────────────────┘
                 │
         ┌───────┴───────┐
         ▼               ▼
   CAN CONNECT      CANNOT CONNECT
   WITHOUT AUTH     WITH AUTH
         │               │
         ├─Fix A          └─Fix B
         │                  │
         ▼                  ▼
   Update config         Create user
   mongodb://           mongosh
   localhost:27017      db.createUser()
         │                  │
         └──────────┬───────┘
                    ▼
           ./gradlew bootRun
                    │
                    ▼
           Test API → 201 OK ✅
```

---

## FIX A: No Authentication

**When**: MongoDB runs without authentication

**Step 1**: Edit `application.properties`
```ini
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
```

**Step 2**: Remove any credentials or authSource

**Step 3**: Rebuild and restart
```bash
./gradlew clean build && ./gradlew bootRun
```

---

## FIX B: Add User

**When**: MongoDB needs credentials but user doesn't exist

**Step 1**: Open MongoDB shell
```bash
mongosh
```

**Step 2**: Create user
```javascript
use admin
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [{ role: "readWrite", db: "carlevdb" }]
})
exit
```

**Step 3**: Verify config has credentials
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

**Step 4**: Rebuild and restart
```bash
./gradlew clean build && ./gradlew bootRun
```

---

## Verification Commands

### Is MongoDB running?
```powershell
Get-Service MongoDB | Select-Object Status
# Status: Running ✅ or Stopped ❌
```

### Can I connect without password?
```bash
mongosh --eval "print('OK')"
# Output: OK ✅ (No password needed)
# Error ❌ (Password needed)
```

### Does user exist?
```bash
mongosh -u carlevuser -p carlevpassword
# Connected ✅ or Error ❌
```

### Is port listening?
```powershell
netstat -ano | findstr :27017
# Output = port open ✅
# No output = port closed ❌
```

---

## Configuration Matrix

| Scenario | Config | Notes |
|----------|--------|-------|
| Local, no auth | `mongodb://localhost:27017/carlevdb` | Fastest for dev |
| Local, with auth | `mongodb://user:pass@localhost:27017/carlevdb?authSource=admin` | Production-like |
| Remote server | `mongodb://user:pass@server-ip:27017/carlevdb?authSource=admin` | Change server-ip |
| MongoDB Atlas | `mongodb+srv://user:pass@cluster.mongodb.net/carlevdb` | Cloud hosted |

---

## Common Error Messages & Fixes

### "Unauthorized"
```
Fix: Create user OR disable auth
mongosh → db.createUser({user:"carlevuser", pwd:"carlevpassword", ...})
```

### "Connection refused"  
```
Fix: Start MongoDB
Start-Service MongoDB
```

### "No credentials supplied"
```
Fix: Add credentials to URI
mongodb://user:pass@localhost:27017/carlevdb?authSource=admin
```

### "Database not found"
```
Fix: MongoDB auto-creates on first write (this is OK)
Or manually: use carlevdb; db.createCollection("test")
```

---

## One-Liner Fixes

### Diagnose everything
```bash
.\diagnose-mongodb.ps1
```

### Create MongoDB user quickly
```bash
mongosh --eval "use admin; db.createUser({user:'carlevuser',pwd:'carlevpassword',roles:[{role:'readWrite',db:'carlevdb'}]})"
```

### Test connection
```bash
mongosh "mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin" --eval "print('OK')"
```

### Restart everything
```bash
Restart-Service MongoDB
./gradlew clean build && ./gradlew bootRun
```

---

## Test API After Fix

```powershell
# Quick test
$body = @{
    title="Test"
    description="Test"
    source="Test"
    url="https://test.com"
    category="Test"
    publishedAt=(Get-Date -Format "o")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/events/new/event" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body
```

**Expected**: HTTP 201 with event ID ✅

---

## File Reference

| Need | File |
|------|------|
| Diagnose issue | `diagnose-mongodb.ps1` |
| Learn more | `SOLUTION_GUIDE.md` |
| Config examples | `APPLICATION_PROPERTIES_EXAMPLES.md` |
| Advanced help | `MONGODB_TROUBLESHOOTING.md` |

---

## Time Estimates

| Task | Time |
|------|------|
| Run diagnostic | 2 min |
| Fix A (no auth) | 3 min |
| Fix B (create user) | 5 min |
| Rebuild app | 3 min |
| Test API | 2 min |
| **Total** | **15 min** |

---

## Emergency Commands

If stuck, run these in order:

```powershell
# 1. Check MongoDB
Get-Service MongoDB | Select-Object Status

# 2. Check port
netstat -ano | findstr :27017

# 3. Try connection
mongosh

# 4. View logs
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 30

# 5. Restart all
Restart-Service MongoDB
./gradlew clean build
./gradlew bootRun
```

---

## Decision Makers

**Use This** | **If** | **Config**
---|---|---
A | MongoDB has NO auth | `mongodb://localhost:27017/carlevdb`
B | MongoDB needs credentials | Create user then use with credentials
B | User already exists | Just verify URI has credentials

---

## Key Point

✅ **Run `.\diagnose-mongodb.ps1` first** - it will tell you EXACTLY what to do

❌ Don't guess - let the diagnostic guide you

🚀 **Then follow the recommended fix in 5-10 minutes**

---

## Support Checklist

- [ ] Ran `.\diagnose-mongodb.ps1`
- [ ] Read the output carefully  
- [ ] Applied the recommended fix
- [ ] Restarted Spring Boot
- [ ] Tested API endpoint
- [ ] Got HTTP 201 response

If all checked ✅ → **You're done!**
If any unchecked ❌ → Check `SOLUTION_GUIDE.md`
