# MongoDB Authentication Fix - Summary

## What Was Done

### 1. ✅ Fixed MongoDB Authentication Configuration
**File Updated**: `src/main/resources/application.properties`

**Change Made**:
```ini
# BEFORE:
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb

# AFTER:
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

**Why This Fixes the Issue**:
- The error "Command update requires authentication" was occurring because MongoDB didn't know which database to authenticate against
- Adding `?authSource=admin` tells MongoDB to authenticate the user against the `admin` database (standard MongoDB auth database)
- This is the critical parameter that was missing

### 2. ✅ Server Port Configuration
- **Port**: `8081` (already configured)
- **API Endpoint**: `POST http://localhost:8081/new/event`

### 3. ✅ Created Sample JSON Request
**File**: `sample-event-request.json`

```json
{
  "id": "evt-12345",
  "title": "Sample Event Title",
  "description": "This is a sample event description",
  "source": "News API",
  "url": "https://example.com/article/12345",
  "category": "Technology",
  "publishedAt": "2026-01-16T13:17:20.059+05:30",
  "metadata": {
    "author": "John Doe",
    "source_id": "news-api-001",
    "confidence_score": 0.95
  },
  "rawData": {
    "original_id": "src-12345",
    "original_source": "external-news-provider",
    "additional_info": "Any additional raw data can be stored here"
  }
}
```

---

## Testing Instructions

### Option 1: Using PowerShell (Recommended for Windows)
```powershell
# Navigate to project directory
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service

# Run the test script
.\test-api.ps1
```

### Option 2: Using Command Prompt (Windows)
```cmd
# Navigate to project directory
cd C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-api-service

# Run the batch file
test-api.bat
```

### Option 3: Using cURL Directly
```bash
curl -X POST http://localhost:8081/new/event \
  -H "Content-Type: application/json" \
  -d @sample-event-request.json
```

### Option 4: Using Postman
1. Open Postman
2. Create NEW request
3. Method: `POST`
4. URL: `http://localhost:8081/new/event`
5. Headers: `Content-Type: application/json`
6. Body (raw, JSON): Copy content from `sample-event-request.json`
7. Click "Send"

---

## MongoDB Prerequisites

### Verify MongoDB Has Authentication Enabled
```bash
# Connect to MongoDB and check:
mongo "mongodb://carlevuser:carlevpassword@localhost:27017/admin"

# If successful, the user exists and MongoDB has authentication enabled
```

### If MongoDB Doesn't Have the User
```javascript
// Connect to admin database
use admin

// Create the user
db.createUser({
  user: "carlevuser",
  pwd: "carlevpassword",
  roles: [
    { role: "readWrite", db: "carlevdb" }
  ]
})
```

### If You Want to Disable Authentication (Development Only)
1. Edit MongoDB config file: `C:\Program Files\MongoDB\Server\[VERSION]\bin\mongod.cfg`
2. Remove or comment out the `security` section
3. Update `application.properties` to: `spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb`
4. Restart MongoDB: `Restart-Service MongoDB`

---

## Files Created

1. **sample-event-request.json** - Sample POST request body
2. **MONGODB_SETUP_AND_API_TESTING.md** - Comprehensive setup and testing guide
3. **test-api.bat** - Windows batch file for automated testing
4. **test-api.ps1** - PowerShell script for automated testing
5. **QUICK_START.md** - This file

---

## Next Steps

### Step 1: Rebuild and Restart Application
```bash
# Clean build
./gradlew clean build

# Run the application
./gradlew bootRun
```

### Step 2: Verify MongoDB Connection
- Check application logs for: `MongoTemplate initialized` or similar success message
- You should NOT see "Unauthorized" errors anymore

### Step 3: Test the API
- Run one of the test scripts above
- Verify you get a 200/201 response with saved event details

### Step 4: Monitor MongoDB
- Check MongoDB logs to confirm successful authentication
- Verify documents are being saved to the `carlevdb` database

---

## Expected Results

### Successful Response
```json
HTTP 200 / 201
{
  "id": "evt-12345",
  "title": "Sample Event Title",
  "status": "COLLECTED",
  "createdAt": "2026-01-16T13:17:20.059+05:30",
  "updatedAt": "2026-01-16T13:17:20.059+05:30"
}
```

### If Still Getting "Unauthorized"
1. Verify credentials: Check that `carlevuser` with password `carlevpassword` exists in MongoDB
2. Check MongoDB is running: `Get-Service MongoDB | Select-Object Status`
3. Verify authentication is enabled in MongoDB config
4. Try connecting directly with MongoDB Compass using the connection string from `application.properties`

---

## Configuration File Changes

### File: src/main/resources/application.properties
```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration with Authentication
# Update username and password with your actual MongoDB credentials
# authSource=admin specifies the authentication database
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
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

---

## Troubleshooting Commands

### Check MongoDB Service Status
```powershell
Get-Service MongoDB | Select-Object Status
```

### Start MongoDB Service
```powershell
Start-Service MongoDB
```

### Stop MongoDB Service
```powershell
Stop-Service MongoDB
```

### Check MongoDB Port Listening
```powershell
netstat -ano | findstr :27017
```

### Test Connection String
```powershell
# Using MongoDB Compass:
# Connection String: mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

---

## Summary of Changes

| Component | Before | After | Status |
|-----------|--------|-------|--------|
| MongoDB URI | `mongodb://...@localhost:27017/carlevdb` | `mongodb://...@localhost:27017/carlevdb?authSource=admin` | ✅ Fixed |
| Server Port | 8081 | 8081 | ✅ OK |
| Sample JSON | N/A | Created | ✅ Created |
| Testing Scripts | N/A | Created | ✅ Created |
| Documentation | N/A | Created | ✅ Created |

---

## Support Resources

- **MongoDB Documentation**: https://docs.mongodb.com/manual/reference/connection-string/
- **Spring Data MongoDB**: https://spring.io/projects/spring-data-mongodb
- **Spring Boot Documentation**: https://spring.io/projects/spring-boot

---

**Last Updated**: January 16, 2026
**Status**: READY FOR TESTING ✅
