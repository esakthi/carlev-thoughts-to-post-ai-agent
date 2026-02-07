# MongoDB Authentication Setup & API Testing Guide

## Issue Resolution Summary

### MongoDB Authentication Error Fixed
**Error**: `Command execution failed on MongoDB server with error 13 (Unauthorized): 'Command update requires authentication'`

**Root Cause**: MongoDB connection string was missing the `authSource=admin` parameter, which tells MongoDB where to authenticate the user.

### Configuration Updated
**File**: `src/main/resources/application.properties`

```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

**Key Parameters**:
- `carlevuser` - MongoDB username
- `carlevpassword` - MongoDB password  
- `localhost:27017` - MongoDB server address and port
- `carlevdb` - Database name
- `authSource=admin` - **CRITICAL** - Specifies the authentication database (usually "admin" for MongoDB)

---

## MongoDB Setup Instructions

### Option 1: Enable Authentication on MongoDB (Recommended for Production)

1. **Connect to MongoDB without auth** (initial setup):
   ```
   mongo mongodb://localhost:27017
   ```

2. **Create an admin user** (if not exists):
   ```javascript
   use admin
   db.createUser({
     user: "admin",
     pwd: "adminpassword",
     roles: ["root"]
   })
   ```

3. **Create application user**:
   ```javascript
   use admin
   db.createUser({
     user: "carlevuser",
     pwd: "carlevpassword",
     roles: [
       { role: "readWrite", db: "carlevdb" }
     ]
   })
   ```

4. **Restart MongoDB with authentication**:
   - **Windows Service**: 
     ```powershell
     Stop-Service MongoDB
     # Modify MongoDB config at: C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg
     # Add: security:
     #        authorization: enabled
     Start-Service MongoDB
     ```

### Option 2: Disable Authentication (Local Development Only)

1. **Stop MongoDB**:
   ```powershell
   Stop-Service MongoDB
   ```

2. **Update MongoDB configuration**:
   - Edit: `C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg`
   - Remove or comment out the `security` section
   - Remove `authSource` from connection string

3. **Update application.properties**:
   ```ini
   spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
   ```

4. **Restart MongoDB**:
   ```powershell
   Start-Service MongoDB
   ```

---

## Testing the API

### Server Port
**Updated to**: `8081`

### Endpoint
```
POST http://localhost:8081/new/event
```

### Sample JSON Request

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

### cURL Command Examples

#### 1. Basic Event Creation
```bash
curl -X POST http://localhost:8081/new/event \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Breaking News",
    "description": "A sample breaking news event",
    "source": "Reuters",
    "url": "https://reuters.com/article/123",
    "category": "World News",
    "publishedAt": "2026-01-16T13:17:20.059+05:30"
  }'
```

#### 2. Event with Full Metadata
```bash
curl -X POST http://localhost:8081/new/event \
  -H "Content-Type: application/json" \
  -d '{
    "id": "evt-001",
    "title": "Technology Breakthrough",
    "description": "New AI breakthrough announced",
    "source": "TechCrunch",
    "url": "https://techcrunch.com/ai-breakthrough",
    "category": "Technology",
    "publishedAt": "2026-01-16T13:17:20.059+05:30",
    "metadata": {
      "author": "John Tech",
      "importance": "high",
      "tags": ["AI", "machine-learning", "breakthrough"]
    },
    "rawData": {
      "original_format": "RSS",
      "processing_time_ms": 150
    }
  }'
```

#### 3. Using PowerShell (Windows)
```powershell
$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    title = "Event from PowerShell"
    description = "Testing from Windows PowerShell"
    source = "Test Source"
    url = "https://example.com"
    category = "Testing"
    publishedAt = (Get-Date -Format "o")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/new/event" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

#### 4. Using Postman
- **Method**: POST
- **URL**: `http://localhost:8081/new/event`
- **Headers**: `Content-Type: application/json`
- **Body**: Use the JSON samples provided above

---

## Expected Response

### Success Response (HTTP 200/201)
```json
{
  "id": "evt-12345",
  "title": "Sample Event Title",
  "status": "COLLECTED",
  "createdAt": "2026-01-16T13:17:20.059+05:30",
  "updatedAt": "2026-01-16T13:17:20.059+05:30"
}
```

### Error Response (HTTP 500)
```json
{
  "error": "MongoDB authentication failed",
  "message": "Command execution failed on MongoDB server with error 13 (Unauthorized)"
}
```

---

## Troubleshooting

### 1. Still Getting "Unauthorized" Error

**Verify MongoDB is running with authentication**:
```powershell
# Check MongoDB logs
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 50
```

**Verify credentials exist**:
```bash
mongo "mongodb://carlevuser:carlevpassword@localhost:27017/admin"
```

### 2. Connection Refused Error

**Check MongoDB service status**:
```powershell
Get-Service MongoDB | Select-Object Status
```

**Start MongoDB if stopped**:
```powershell
Start-Service MongoDB
```

### 3. Database Not Found Error

**Create the database**:
```javascript
use carlevdb
db.createCollection("topic_events")
```

### 4. Verify Connection String

**Test in MongoDB Compass**:
1. Open MongoDB Compass
2. Use connection string: `mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin`
3. Connect

---

## Next Steps

1. ✅ Update `application.properties` with `authSource=admin` - **DONE**
2. ✅ Port configured to `8081` - **DONE**
3. ⏳ Verify MongoDB has user `carlevuser` with correct password
4. ⏳ Restart Spring Boot application
5. ⏳ Test the `/new/event` endpoint with provided sample JSON
6. ⏳ Check logs for successful database operations

---

## Additional Configuration Options

### Connection Pool Settings (Optional)
```ini
spring.data.mongodb.max-pool-size=10
spring.data.mongodb.min-pool-size=5
spring.data.mongodb.server-selection-timeout=5000
spring.data.mongodb.socket-timeout=30000
```

### Replica Set Configuration (If Using Replica Set)
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017,localhost:27018,localhost:27019/carlevdb?authSource=admin&replicaSet=rs0
```

---

## Reference Files

- **Configuration**: `src/main/resources/application.properties`
- **Sample Request**: `sample-event-request.json`
- **Service**: `src/main/java/com/carlev/api/service/EventService.java`
- **Controller**: `src/main/java/com/carlev/api/controller/EventController.java`
