# AutoBuild

AutoBuild is a production-grade continuous integration and deployment (CI/CD) orchestration platform built with Spring Boot 3, Java 21, and a Node.js sidecar runner architecture. It enables users to register GitHub repositories, configure build/test/deploy stages, parse execution commands dynamically from a `pipelineforge.yml` configuration file, and execute processes inside an isolated container environment.

---

## System Architecture

The platform is designed around a modular monolith architecture with containerized infrastructure dependencies.

```
┌─────────────────────────────────────────────────────────────┐
│  Docker Compose Environment                                 │
│                                                             │
│  ┌──────────────────┐        ┌──────────────────────────┐   │
│  │  pipelineforge-  │        │  pipelineforge-runner     │   │
│  │  app (Java 21)   │        │  (Node.js 20)             │   │
│  │                  │        │                           │   │
│  │  - Downloads repo│docker  │  - Runs npm install       │   │
│  │    to shared vol │ exec   │  - Runs npm test          │   │
│  │  - Orchestrates  │───────>│  - Runs npm start         │   │
│  │    stages        │        │  - Express app on :3000   │   │
│  │  :8080           │        │  :3000                    │   │
│  └───────┬──────────┘        └───────┬──────────────────┘   │
│          │                           │                      │
│          └────── shared volume ──────┘                      │
│                /shared/deployments                           │
└─────────────────────────────────────────────────────────────┘
```

### Components

1. **AutoBuild Engine (Spring Boot App)**: Serves the REST API, manages database persistence, streams live console outputs, and orchestrates stage tasks.
2. **Runner Sidecar (Node.js)**: Runs in a separate lightweight container (`node:20-slim`). It receives commands from the main app container via the Docker daemon socket and runs them inside its filesystem.
3. **Shared Volume**: A Docker named volume (`deployments`) mounted at `/shared/deployments` in both containers. The Java app downloads the repository code archive into this volume, making it instantly accessible to the Node.js runner.
4. **Database (PostgreSQL)**: Persists user accounts, repository registrations, pipelines, stage execution statuses, and deployment run histories.
5. **Cache & Rate Limiting (Redis)**: Manages API rate limiting bucket-tokens, caches active pipeline statuses, and stores deployment metadata.
6. **Message Broker (RabbitMQ)**: Offloads pipeline execution jobs to background workers asynchronously, ensuring reliability under high request volume.

---

## Features

### 1. Dynamic YAML Pipeline Execution
- **YAML Parser**: Reads a `pipelineforge.yml` file from the root of the connected GitHub repository.
- **Secure Downloader**: Connects to the GitHub API, downloads the repository source zip archive, and extracts it to the shared deployments volume.
- **Dynamic Command Runner**: Executes commands defined in the YAML file (such as `npm install`, `npm test`, or `npm start`) sequentially.
- **Detached Execution**: Deploys the startup commands (e.g. `npm start`) in detached mode inside the runner sidecar. It captures process IDs (PIDs) and monitors logs to verify the application process remains alive.
- **Runner Health Verification**: Performs curl health check runs against the runner to guarantee the application started successfully on port 3000.

### 2. Observing & Centralized Logging
- **Structured Console & JSON Logging**: Features local dev profile log formats and Logstash-ready structured JSON log outputs for production aggregations.
- **Actuator Endpoint Metrics**: Exposes system performance and Prometheus metrics under `/actuator`. Includes a custom database latency health indicator.
- **Trace Propagation**: Injects unique tracing/correlation IDs (`traceId`) on incoming HTTP requests. These IDs propagate automatically across virtual thread executors and asynchronous workers using custom task decorators.

### 3. API Rate Limiting
- **Redis-Backed Limiting**: Limits client request volumes dynamically based on authenticated usernames or client IP addresses.
- **Graceful Fail-Open**: If Redis becomes unavailable, the rate limiting filter fails open, ensuring no service disruption for legitimate traffic.

### 4. Developer Dashboard SPA
- Served directly from the Spring Boot static classpath (`/index.html`).
- Built using Outfit/JetBrains Mono typography, modern flat geometric styles, and bento-box layouts.
- Completely square aesthetics (no rounded corners) and flat solid background colors.
- Features real-time log polling, interactive repository registration, pipeline creation, and a rollback trigger console.

---

## YAML Configuration File Format

To run pipelines dynamically, the target GitHub repository must contain a `pipelineforge.yml` file at the root.

```yaml
stages:
  build: npm install
  test: npm test
  deploy: npm start
```

Supported stages:
- **build**: Synchronous preparation step (e.g. installing dependencies).
- **test**: Synchronous verification step (e.g. running test suites).
- **deploy**: Long-running process execution (e.g. starting a server).

---

## Getting Started

### Prerequisites
- Docker Desktop or Docker Compose
- Maven 3.8+ (for running locally outside Docker)
- JDK 21 (for running locally outside Docker)

### Local Configuration
1. Clone the repository to your local machine.
2. Review the environment variables in the `.env` file at the root:
   ```env
   SPRING_PROFILES_ACTIVE=dev
   POSTGRES_DB=pipelineforge
   POSTGRES_USER=pipelineforge
   POSTGRES_PASSWORD=pipelineforge
   RABBIT_USERNAME=guest
   RABBIT_PASSWORD=guest
   JWT_SECRET=change-me-change-me-change-me-change-me
   ENCRYPTION_SECRET=change-me-change-me-change-me-change-me
   ```

### Running with Docker Compose
To build and start the entire stack (App, Runner Sidecar, Postgres, Redis, RabbitMQ):

```bash
docker compose up -d --build
```

- **Developer Dashboard**: `http://localhost:8080/`
- **OpenAPI Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **RabbitMQ Management Console**: `http://localhost:15672` (Credentials: `guest` / `guest`)

---

## API Endpoints

All core API endpoints are prefixed with `/api/v1`.

### Authentication
- `POST /api/v1/auth/register`: Create a new user account (Roles: `ADMIN`, `DEVELOPER`, `RELEASE_MANAGER`).
- `POST /api/v1/auth/login`: Authenticate and obtain a JWT bearer token.

### Repositories
- `GET /api/v1/repositories`: List registered repositories.
- `POST /api/v1/repositories`: Register a new repository.
- `DELETE /api/v1/repositories/{id}`: Unregister a repository.

### Pipelines
- `GET /api/v1/pipelines`: List pipelines.
- `POST /api/v1/pipelines`: Create a pipeline defining stage execution orders.
- `POST /api/v1/pipelines/{id}/trigger`: Trigger a manual execution run.

### Deployments
- `GET /api/v1/deployments`: Query deployment histories.
- `GET /api/v1/deployments/{id}`: Query detailed deployment metadata.
- `GET /api/v1/deployments/{id}/logs`: Retrieve real-time terminal stdout/stderr logs.
- `POST /api/v1/deployments/rollbacks`: Rollback a pipeline to a previous successful version.

---

## Deployment to Production

Due to the use of Docker socket mounting and shared volumes, AutoBuild cannot be deployed directly to standard PaaS engines like Render as a single Compose file. The recommended production deployment approaches are:

1. **Virtual Private Server (VPS)**:
   Deploy the Docker Compose stack onto a managed VM (such as an AWS EC2 instance, GCP Compute Engine instance, or DigitalOcean Droplet) with Docker installed, which allows full access to `/var/run/docker.sock`.
2. **AWS ECS (Elastic Container Service)**:
   Deploy the App and Runner containers as a single Task Definition using EC2 launch types. Map shared directories using ECS Bind Mounts and mount the Docker socket from the host.
3. **Kubernetes**:
   Run the App and Runner sidecar in the same Pod, sharing storage using a local `emptyDir` volume. Transition container communication to an internal API agent over `localhost` instead of using the Docker socket.
#   A u t o B u i l d  
 