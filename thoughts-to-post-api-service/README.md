# Thoughts-to-Post API Service

Spring Boot microservice that serves as the backend for the Thoughts-to-Post AI Agent Platform.

## Features

- **REST API** for Angular UI communication
- **MongoDB** persistence with version tracking and audit history
- **Kafka Integration** for async communication with AI Agent
- **LinkedIn API** integration for social media posting
- **Extensible** architecture for Facebook/Instagram support

## Prerequisites

- Java 21+
- Gradle 8.5+ (or use included wrapper)
- MongoDB (running on localhost:27017)
- Kafka (running on localhost:9092)

## Configuration

Copy and configure environment variables or update `application.yml`:

```bash
# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@localhost:27017/thoughtstopost?authSource=admin

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# LinkedIn OAuth
LINKEDIN_CLIENT_ID=your-client-id
LINKEDIN_CLIENT_SECRET=your-client-secret
LINKEDIN_REDIRECT_URI=http://localhost:8080/api/oauth/linkedin/callback
```

## Running

```bash
# Using Gradle wrapper (recommended)
./gradlew bootRun

# Or on Windows
gradlew.bat bootRun

# Build JAR
./gradlew bootJar
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/thoughts | Create new thought |
| GET | /api/thoughts/{id} | Get thought by ID |
| GET | /api/thoughts | Get user's thoughts |
| GET | /api/thoughts/{id}/history | Get thought history |
| POST | /api/thoughts/{id}/approve | Approve and post |
| POST | /api/thoughts/{id}/reject | Reject thought |
| GET | /api/oauth/linkedin/authorize | Start LinkedIn OAuth |
| GET | /api/oauth/linkedin/callback | LinkedIn OAuth callback |

## TODO

- [ ] Configure MongoDB connection
- [ ] Configure Kafka connection
- [ ] Set up LinkedIn Developer App and OAuth credentials
- [ ] Implement user authentication (JWT)
- [ ] Add Facebook integration
- [ ] Add Instagram integration
- [ ] Upload images to cloud storage instead of base64
