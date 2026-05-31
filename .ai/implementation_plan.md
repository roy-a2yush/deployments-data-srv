# Implementation Plan - Deployment Event Data Service (Revised)

This plan outlines the architecture, data model, API contract, and seeding mechanism for the deployment event data service, updated in response to code review feedback.

---

## Modular API Design (`api-spec/`)

We will restructure the OpenAPI specification to be modular by breaking out schemas, examples, and responses into dedicated directories under the `api-spec` folder with strict success/failure separation:

```text
api-spec/
├── openapi.json                 # Main entrypoint
├── schemas/
│   ├── deployment.json          # Deployment model schema
│   ├── error.json               # Standardized Error schema
│   └── offset-pagination-params.json # Centralized standard pagination properties
├── examples/
│   ├── success/
│   │   └── deployment.json      # Example of a deployment
│   └── failure/
│       ├── error-400.json       # Example 400 Bad Request
│       ├── error-404.json       # Example 404 Not Found
│       └── error-409.json       # Example 409 Conflict
└── responses/
    ├── success/
    │   ├── deployments-list.json  # GET /deployments response
    │   └── deployment-detail.json # GET /deployments/{id} response
    └── failure/
        ├── error-bad-request.json # 400 Bad Request response
        ├── error-not-found.json   # 404 Not Found response
        └── error-conflict.json    # 409 Conflict response
```

All sub-files will be referenced in [openapi.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/openapi.json) using local `$ref` URIs.

---

## Data Modeling & Seeding (MySQL + Liquibase SQL)

### Schema Definition (`db.changelog-1.0.yaml`)
Create the table with strict column types. `status` and `environment` will be verified using Java Enums and OpenAPI validations.

```sql
CREATE TABLE deployments (
    id VARCHAR(50) PRIMARY KEY,
    service VARCHAR(100) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    duration INT NOT NULL,
    timestamp DATETIME NOT NULL,
    commit_sha VARCHAR(50) NOT NULL,
    previous_commit_sha VARCHAR(50),
    deployed_by VARCHAR(100) NOT NULL,
    failure_reason TEXT,
    rollback_of VARCHAR(50),
    INDEX idx_service_env_status (service, environment, status),
    INDEX idx_timestamp (timestamp)
);
```

### Seeding Strategy (`db.changelog-2.0-seed.yaml`)
We will write a dedicated **Liquibase migration step (`db.changelog-2.0-seed.yaml`)** to insert **200+ mock deployment events** directly using SQL `INSERT` commands.
- We will seed permutations of services (`auth-service`, `billing-api`, `notification-worker`, `frontend-dashboard`, `search-indexer`), environments (`production`, `staging`, `canary`), statuses, realistic run durations, and timestamps spread across a 30-day window.

---

## Core Functional Requirements

### 1. Paginated Endpoint Filtering
- The `GET /deployments` endpoint will natively support **pagination (`page`, `size`)** as a core functional requirement to handle our 200+ seeded records efficiently.
- Supports sorting by timestamp (descending by default) to surface the most recent deployments first.

### 2. Service Interface Loose Coupling
To ensure the system is modular and easily testable, we will introduce a **loose-coupling interface pattern** for our business logic layer:
- **`DeploymentService` (Interface)**: Defines the functional contract for querying, creating, and retrieving deployments.
- **`DeploymentServiceImpl` (Class)**: Implements the interface using Spring Data JPA.

---

## Testing Strategy & Pyramid Design

To guarantee long-term stability and compatibility with continuous integration (CI) pipelines, we will establish a structured testing pyramid:

```text
       [ E2E System Tests ]         <- Full App context + Testcontainers (future)
     [ Integration/Controller ]     <- MockMvc, Spring Security, Validation (400/404)
    [ Isolated Java Unit Tests ]    <- Mockito, isolated logic assertions
```

1. **Unit Testing (`DeploymentServiceTest.java`)**:
   - Focuses strictly on business logic inside `DeploymentServiceImpl` in total isolation.
   - Mocks the JPA Repository layer using JUnit 5 + Mockito.
   - Validates service-to-dto mapping, pagination offset rules, and correct exception propagation (e.g. throwing custom errors on missing rollbacks).

2. **Integration & Functional Testing (`DeploymentControllerTest.java`)**:
   - Focuses on the web layer and Spring context mappings.
   - Leverages Spring Boot's **`MockMvc`** to execute mock HTTP REST calls against endpoints.
   - Verifies **OpenAPI schema conformity**, **validation checks** (triggering `400 Bad Request` on invalid enum strings), **authentication barriers**, and standard error response mapping.

---

## Incremental Upgrades & Extensibility Plan

### 1. Input Sanity & Anomaly Prevention (Data Quality Safeguards)
* **Duration Boundary Limits**: Set strict logic boundaries on deployment time (e.g., `duration > 0` and `duration < 7200` seconds).
* **Temporal Integrity Check**: Ensure the deployment `timestamp` is not set in the future.
* **Logical Rollback Validation**: Ensure `rollback_of` target exists and is currently `FAILED`.
* **Bombardment & Deduplication Guards**: Idempotent writes to absorb duplicate update payloads from CI/CD pipeline retries.

### 2. High-Scale Architecture Upgrades
* **Asynchronous Ingestion via Kafka**: Configure a consumer to process stream events asynchronously.
* **Performance Caching via Redis**: Cache active `/metrics` or paginated query outputs to dramatically scale reads.

---

## Proposed Changes

### Modular API Contract
#### [NEW] [deployment.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/schemas/deployment.json)
#### [NEW] [error.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/schemas/error.json)
#### [NEW] [offset-pagination-params.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/schemas/offset-pagination-params.json)
#### [NEW] [deployment.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/examples/success/deployment.json)
#### [NEW] [error-400.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/examples/failure/error-400.json)
#### [NEW] [error-404.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/examples/failure/error-404.json)
#### [NEW] [error-409.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/examples/failure/error-409.json)
#### [NEW] [deployments-list.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/responses/success/deployments-list.json)
#### [NEW] [deployment-detail.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/responses/success/deployment-detail.json)
#### [NEW] [error-bad-request.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/responses/failure/error-bad-request.json)
#### [NEW] [error-not-found.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/responses/failure/error-not-found.json)
#### [NEW] [error-conflict.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/responses/failure/error-conflict.json)
#### [MODIFY] [openapi.json](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/api-spec/openapi.json)

### Database Migrations
#### [NEW] [db.changelog-1.0.yaml](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/resources/db/changelog/db.changelog-1.0.yaml)
#### [NEW] [db.changelog-2.0-seed.yaml](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/resources/db/changelog/db.changelog-2.0-seed.yaml)
#### [MODIFY] [db.changelog-master.yaml](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/resources/db/changelog/db.changelog-master.yaml)

### Backend Application Code
#### [NEW] [DeploymentEnvironment.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentEnvironment.java)
#### [NEW] [DeploymentStatus.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentStatus.java)
#### [NEW] [Deployment.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/Deployment.java)
#### [NEW] [DeploymentEnvironmentConverter.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentEnvironmentConverter.java)
#### [NEW] [DeploymentStatusConverter.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentStatusConverter.java)
#### [NEW] [DeploymentRepository.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/repository/DeploymentRepository.java)
#### [NEW] [DeploymentSpecification.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/repository/DeploymentSpecification.java)
#### [NEW] [TelemetryInterceptor.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/interceptor/TelemetryInterceptor.java)
#### [NEW] [WebConfig.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/config/WebConfig.java)
#### [NEW] [DeploymentService.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/service/DeploymentService.java)
#### [NEW] [DeploymentServiceImpl.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/service/DeploymentServiceImpl.java)
#### [NEW] [DeploymentController.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/controller/DeploymentController.java)
#### [NEW] [GlobalExceptionHandler.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/exception/GlobalExceptionHandler.java)

### Test Suite Files
#### [NEW] [DeploymentServiceTest.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/test/java/com/zephyr/deployments_data_srv/service/DeploymentServiceTest.java)
Unit tests for isolated service implementation logic.

#### [NEW] [DeploymentControllerTest.java](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/test/java/com/zephyr/deployments_data_srv/controller/DeploymentControllerTest.java)
Integration tests using MockMvc for HTTP endpoints, filters, enums, security, and exception mappings.

---

## Verification Plan

### Automated Verification
- Run `mvn clean compile` to check OpenAPI generation.
- Execute unit and integration tests: `mvn clean test`.

### Manual Verification
- Test using curl:
  - Paginated list: `curl -u admin:adminpassword http://localhost:8080/deployments?page=0&size=10`
  - Filtered: `curl -u admin:adminpassword http://localhost:8080/deployments?status=failed&page=0&size=5`
