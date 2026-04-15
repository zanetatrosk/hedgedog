# be-dance-app

Backend API for a dance events app (Kotlin + Spring Boot + PostgreSQL + MinIO).

## Prerequisites

- Java 17
- Docker and Docker Compose
- Bash shell (commands below use bash)

## Configuration

This project reads Google OAuth credentials from environment variables.

1. Copy `.env.example` to `.env`
2. Fill values for:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`

```bash
cp .env.example .env
```

## Run Options

### Option 1: Run everything with Docker Compose

Starts backend + PostgreSQL + MinIO in containers.

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

## Useful Endpoints

- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
  - user: `minioadmin`
  - password: `minioadmin`

## Run Tests

```bash
./gradlew test
```

## Build

```bash
./gradlew build
```

