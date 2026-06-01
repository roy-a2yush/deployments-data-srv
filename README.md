# Deployments Data Service 🚀

An enterprise-grade, high-performance **Deployment Event Telemetry Data Service** built on Spring Boot 4.x. This microservice ingests, indexes, and queries paginated deployment metadata across diverse cloud environments while integrating native OpenTelemetry observability and dynamic JPA indexing patterns.

---

## 🛠 Tech Stack
* **Framework**: Spring Boot 4.0.6 (Java 26)
* **Build Tool**: Maven
* **Database**: MySQL 8.0 (Schema provisioning and seeding managed by Liquibase SQL Migrations)
* **Caching & Streaming**: Redis Cache + Apache Kafka (KRaft mode)
* **Observability**: OpenTelemetry (OTel) Collector + Jaeger Tracing (OTLP/HTTP & OTLP/gRPC)
* **Logging**: Logback + Native Spring Boot 4.0 Structured JSON (Logstash format)

---

## 🎨 Dynamic Design Ideas & Architecture Decoupling

This application features several premium architectural patterns to enforce clean decoupling, high database speed, and observability standards:

### **1. Modular OpenAPI 3.0 Specs (`api-spec/`)**
The REST contract is strictly modularized to support team-based changes with zero conflicts:
* Schemas (`api-spec/schemas/`), Examples (`api-spec/examples/`), and Responses (`api-spec/responses/`) are split into clean folder segments.
* Standardized pagination parameters are unified under a single schema file (`offset-pagination-params.json`).
* Inlines, refs, and sub-JSON files are compiled cleanly using a custom resolving bundler into `openapi-bundled.json` during the Maven build phase.

### **2. Dynamic JPA Specification Fluent Builder Pattern**
To scale queries to millions of rows, we migrated from legacy static `@Query` strings containing inefficient `OR` clauses to a dynamic **JPA Specification layer** ([`DeploymentSpecification`](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/repository/DeploymentSpecification.java)):
* **Database Optimization**: If a filter is absent, the compiled SQL contains no overhead conditions, allowing the MySQL optimizer to traverse composite indexes (`idx_service_env_status`) rather than falling back to slow full-table scans.
* **Fluent Builder Pattern**: Supports dynamic method chaining:
  ```java
  Specification<Deployment> spec = DeploymentSpecification.builder()
      .withService(service)
      .withStatus(status)
      .withEnvironment(env)
      .optionalAnd(isAdmin, customSpec)
      .build();
  ```

### **3. Safe JPA Enum Attribute Converters**
To resolve naming convention mismatches between Java (strict uppercase enum constants `CANARY`, `SUCCESS`) and Database/OpenAPI (strict lowercase strings `'canary'`, `'success'`), we introduced JPA Attribute Converters:
* [`DeploymentEnvironmentConverter`](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentEnvironmentConverter.java) and [`DeploymentStatusConverter`](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/model/DeploymentStatusConverter.java) dynamically intercept SQL operations.
* **Result**: Promotes strict, elegant uppercase conventions inside Java code, keeps lowercase entries in database records, and remains 100% compliant with the REST API contract.

### **4. Interceptor-Level OTel Tracing & SLF4J MDC Injection**
We decoupled telemetry tracking completely from business services using a Spring `HandlerInterceptor` ([`TelemetryInterceptor`](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/src/main/java/com/zephyr/deployments_data_srv/interceptor/TelemetryInterceptor.java)):
* **Trace Propagation**: Captures incoming OpenTelemetry traces or programmatically bootstraps a new span context if none exists.
* **MDC Correlation**: Seamlessly binds `trace_id` and `span_id` to SLF4J's Mapped Diagnostic Context. 
* **Zero Boilerplate**: The service layer focuses purely on business logic, while **every single log message** in the application automatically prints the current OTel transaction context!

### **5. Native OTel Structured JSON Logging**
Configured natively in `application.yml` using Spring Boot 4.0's structured console log layout:
* Outputs logs as single-line JSON records formatted in **Logstash** style.
* Automatically includes `trace_id`, `span_id`, `@timestamp`, `thread_name`, and `level` as root keys—ready to be ingested by Fluentbit or vector pipelines.

---

## 🚀 Getting Started

> [!IMPORTANT]
> **Hard Dependency**: **Docker & Docker Compose** are strict, mandatory prerequisites for this project. They are required to orchestrate the backend database, cache layer, messaging queue, OpenTelemetry collector, Jaeger visualizer, and the application itself cohesively (fully optimized for both Apple Silicon arm64 and x86_64 architectures).

---

### **Option A: Run the Entire Stack Cohesively (Recommended)**
To spin up all services, compile the Spring Boot application container, and run the entire telemetry environment cohesively, simply run:
```bash
docker compose up
```
Or run in detached (background) mode:
```bash
docker compose up -d
```
If you make changes to the source code and want to force a clean container rebuild while launching:
```bash
docker compose up --build
```
* The Spring Boot microservice will be available at **`http://localhost:8080`** (serving a premium glassmorphic dark-mode Welcome Page with direct navigation links to Swagger UI).
* Traces will automatically flow into Jaeger on **`http://localhost:16686`**.
* Standardized 404 RFC 7807 errors are handled gracefully (e.g. accessing `/hello` or invalid routes returns a clean payload instead of a 500 error).
* Spring Security has been completely removed to provide zero-overhead, seamless access to endpoints and the Swagger UI.

---

### **Option B: Run Infrastructure + Local Application (Development Mode)**

#### **1. Start the Docker Infrastructure Stack**
Start only the backing databases, brokers, and telemetry collectors:
```bash
docker compose up -d mysql redis kafka otel-collector jaeger
```

* **MySQL**: Port `3306` (Pre-seeded with 210 realistic deployments).
* **Redis**: Port `6379`.
* **Kafka**: Port `9092`.
* **OTel Collector**: Ports `4317` (gRPC) & `4318` (HTTP).
* **Jaeger Web UI**: Port `16686`.

#### **2. Compile and Verify the App**
To compile generated specs and run all unit and controller integration tests:
```bash
mvn clean test
```

#### **3. Start the Microservice Locally**
Start the Spring Boot process on your host machine:
```bash
mvn spring-boot:run
```
The service will start on **`http://localhost:8080`**.

---

## 📊 Visualizing Traces in Jaeger

1. **Trigger Request Traffic**:
   ```bash
   curl "http://localhost:8080/deployments?service=auth-service"
   curl "http://localhost:8080/deployments/deploy_001"
   ```
2. **Access Jaeger UI**:
   Navigate to **[http://localhost:16686](http://localhost:16686)** in your browser.
3. **Filter and Search**:
   * Select **`deployments-data-srv`** (or the span entry `http.request`) from the **Service** dropdown.
   * Click **Find Traces** to view full request lifecycles, database query execution spans, and correlation structures!

---

## 📂 Project Documentation & Artifacts
* **[Implementation Plan (Detailed Architecture Design)](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/.ai/implementation_plan.md)**
* **[Walkthrough (Release Notes & Verification Specs)](file:///Users/a0r1427/Development/Personal/java-spring/deployments-data-srv/.ai/walkthrough.md)**
