# Alternative application.properties Configurations

This file contains different configuration options for various MongoDB setups.

---

## Configuration 1: NO AUTHENTICATION (Local Development)

**Use this if**: MongoDB is running without authentication enabled

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

---

## Configuration 2: WITH AUTHENTICATION (Recommended)

**Use this if**: MongoDB is running with authentication and user 'carlevuser' exists

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - WITH AUTHENTICATION
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

## Configuration 3: DIFFERENT CREDENTIALS

**Use this if**: You're using different MongoDB credentials

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - CUSTOM CREDENTIALS
# Replace YOUR_USERNAME and YOUR_PASSWORD with actual credentials
spring.data.mongodb.uri=mongodb://YOUR_USERNAME:YOUR_PASSWORD@localhost:27017/carlevdb?authSource=admin
spring.data.mongodb.auto-index-creation=true

# Rest of configuration...
```

---

## Configuration 4: DIFFERENT DATABASE NAME

**Use this if**: Your MongoDB database is not named 'carlevdb'

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - DIFFERENT DATABASE
# Replace 'mydatabase' with your actual database name
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/mydatabase?authSource=admin
spring.data.mongodb.auto-index-creation=true

# Rest of configuration...
```

---

## Configuration 5: REMOTE MongoDB SERVER

**Use this if**: MongoDB is running on a different machine

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - REMOTE SERVER
# Replace 'mongodb-server-ip' with actual IP or hostname
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@mongodb-server-ip:27017/carlevdb?authSource=admin
spring.data.mongodb.auto-index-creation=true

# Rest of configuration...
```

Examples:
```ini
# Example with IP address
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@192.168.1.100:27017/carlevdb?authSource=admin

# Example with hostname
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@mongodb.example.com:27017/carlevdb?authSource=admin
```

---

## Configuration 6: MONGODB ATLAS (Cloud)

**Use this if**: Using MongoDB Atlas cloud service

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - MONGODB ATLAS
# Get connection string from MongoDB Atlas dashboard
# Format: mongodb+srv://username:password@cluster-name.mongodb.net/database-name?retryWrites=true&w=majority
spring.data.mongodb.uri=mongodb+srv://carlevuser:carlevpassword@cluster0.abc123.mongodb.net/carlevdb?retryWrites=true&w=majority
spring.data.mongodb.auto-index-creation=true

# Rest of configuration...
```

---

## Configuration 7: WITH CONNECTION POOL SETTINGS

**Use this if**: You need custom connection pooling

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - WITH CONNECTION POOL
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin&maxPoolSize=10&minPoolSize=5
spring.data.mongodb.auto-index-creation=true

# Additional Connection Pool Settings
spring.data.mongodb.max-pool-size=10
spring.data.mongodb.min-pool-size=5
spring.data.mongodb.server-selection-timeout=5000
spring.data.mongodb.socket-timeout=30000

# Kafka Producer Configuration...
```

---

## Configuration 8: WITH SSL/TLS (Production)

**Use this if**: Connecting to MongoDB with SSL certificate

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - WITH SSL/TLS
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin&ssl=true&retryWrites=true
spring.data.mongodb.auto-index-creation=true

# SSL Configuration
spring.data.mongodb.ssl-enabled=true

# Kafka Producer Configuration...
```

---

## Configuration 9: USING ENVIRONMENT VARIABLES

**Use this if**: You want to keep credentials secure using environment variables

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - USING ENVIRONMENT VARIABLES
# Set these environment variables before running:
# MONGODB_USERNAME=carlevuser
# MONGODB_PASSWORD=carlevpassword
# MONGODB_HOST=localhost
# MONGODB_PORT=27017
# MONGODB_DATABASE=carlevdb

spring.data.mongodb.uri=mongodb://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@${MONGODB_HOST}:${MONGODB_PORT}/${MONGODB_DATABASE}?authSource=admin
spring.data.mongodb.auto-index-creation=true

# Kafka Producer Configuration...
```

**Set environment variables (PowerShell)**:
```powershell
$env:MONGODB_USERNAME="carlevuser"
$env:MONGODB_PASSWORD="carlevpassword"
$env:MONGODB_HOST="localhost"
$env:MONGODB_PORT="27017"
$env:MONGODB_DATABASE="carlevdb"

./gradlew bootRun
```

---

## Configuration 10: REPLICA SET

**Use this if**: Using MongoDB Replica Set

```ini
spring.application.name=carlev-api-service
server.port=8081

# MongoDB Configuration - REPLICA SET
# Multiple servers separated by commas
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@mongo1:27017,mongo2:27017,mongo3:27017/carlevdb?authSource=admin&replicaSet=rs0
spring.data.mongodb.auto-index-creation=true

# Kafka Producer Configuration...
```

---

## How to Use These Configurations

1. **Identify your MongoDB setup**
   - Is it local or remote?
   - Does it have authentication?
   - What's the database name?

2. **Find the matching configuration** above

3. **Update your `application.properties`** file:
   ```bash
   # Edit the file
   notepad src/main/resources/application.properties
   
   # Replace the MongoDB configuration with the appropriate one
   ```

4. **Restart Spring Boot**:
   ```bash
   ./gradlew clean build
   ./gradlew bootRun
   ```

5. **Test the API** to verify it works

---

## Troubleshooting by Scenario

### Scenario: "Connection refused"
- MongoDB is not running
- Wrong hostname/port in URI
- Check: `netstat -ano | findstr :27017`

### Scenario: "Unauthorized - authentication failed"
- Wrong credentials in URI
- User doesn't exist in MongoDB
- Check: Run `mongosh` with credentials manually

### Scenario: "Authentication required"
- You need authentication but credentials not provided in URI
- Add credentials to URI: `mongodb://user:pass@...?authSource=admin`

### Scenario: "Database not found"
- Database doesn't exist (normal, will be created on first write)
- Wrong database name in URI
- Check: `use carlevdb` in MongoDB shell

---

## Quick Reference: URI Parameters

| Parameter | Purpose | Example |
|-----------|---------|---------|
| `username:password` | Credentials | `carlevuser:carlevpassword` |
| `host:port` | Server address | `localhost:27017` |
| `/database` | Database name | `/carlevdb` |
| `authSource=admin` | Auth database | `?authSource=admin` |
| `ssl=true` | Enable SSL | `?ssl=true` |
| `maxPoolSize=10` | Connection pool | `?maxPoolSize=10` |
| `replicaSet=rs0` | Replica set name | `?replicaSet=rs0` |
| `retryWrites=true` | Retry writes | `?retryWrites=true` |

---

## Recommended Configurations

### For Local Development (Fastest)
```ini
spring.data.mongodb.uri=mongodb://localhost:27017/carlevdb
```

### For Team Development (With Auth)
```ini
spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
```

### For Production (Secure)
```ini
spring.data.mongodb.uri=mongodb+srv://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@cluster0.mongodb.net/carlevdb?retryWrites=true&w=majority
```

---

## Testing Your Configuration

After updating `application.properties`, test with PowerShell:

```powershell
# Test script
$testUri = "mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin"

Write-Host "Testing MongoDB connection..."
try {
    mongosh $testUri --eval "print('Connected successfully')"
    Write-Host "✓ Connection successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Connection failed: $_" -ForegroundColor Red
}
```
