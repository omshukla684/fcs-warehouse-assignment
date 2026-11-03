# FCS Warehouse Management System

A production-ready Quarkus-based warehouse management API built following **Hexagonal Architecture** (Ports & Adapters). This project implements the FnCS case study covering warehouse lifecycle management, store operations, and fulfillment orchestration.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Setup](#local-setup)
  - [Running the Application](#running-the-application)
- [API Reference](#api-reference)
  - [Warehouse API](#warehouse-api)
  - [Store API](#store-api)
  - [Fulfillment API](#fulfillment-api)
- [Business Rules](#business-rules)
- [Testing & Coverage](#testing--coverage)
- [CI/CD Pipeline](#cicd-pipeline)
- [Design Decisions](#design-decisions)

---

## Architecture Overview

The application follows the **Hexagonal Architecture (Ports & Adapters)** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    Inbound Adapters                      │
│          (REST Resources / JAX-RS Controllers)           │
└──────────────────────┬──────────────────────────────────┘
                       │  uses
┌──────────────────────▼──────────────────────────────────┐
│                     Domain Layer                         │
│  ┌──────────────┐  ┌───────────────┐  ┌─────────────┐  │
│  │  Use Cases   │→ │  Validators   │  │    Models   │  │
│  │ (Orchestrate)│  │ (Business     │  │  (Entities) │  │
│  └──────┬───────┘  │  Rules)       │  └─────────────┘  │
│         │          └───────────────┘                    │
│  ┌──────▼───────────────────────────────────────────┐   │
│  │              Ports (Interfaces)                  │   │
│  │  WarehouseStore │ LocationResolver │ FulfillmentStore │
│  └──────────────────────┬───────────────────────────┘   │
└──────────────────────────┼──────────────────────────────┘
                           │ implements
┌──────────────────────────▼──────────────────────────────┐
│                  Outbound Adapters                        │
│         (JPA Repositories / External Gateways)           │
└─────────────────────────────────────────────────────────┘
```

**Key Packages:**
| Package | Role |
|---------|------|
| `warehouses.domain.models` | Pure domain entities (no JPA annotations) |
| `warehouses.domain.ports` | Input/output port interfaces |
| `warehouses.domain.usecases` | Application use cases (business orchestration) |
| `warehouses.domain.validator` | Extracted business-rule validation classes |
| `warehouses.adapters.database` | JPA entities + repository implementations |
| `warehouses.adapters.restapi` | JAX-RS REST resource implementations |
| `warehouses.fulfillment.event` | CDI domain events for fulfillment |
| `stores` | Store domain (Panache Active Record + CDI events) |

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Quarkus 3.13.3 |
| Language | Java 17 |
| ORM | Hibernate ORM Panache |
| Database | PostgreSQL 16 |
| REST | RESTEasy (JAX-RS) / OpenAPI Generator |
| API Docs | SmallRye OpenAPI + Swagger UI |
| Testing | JUnit 5 + Mockito |
| Coverage | JaCoCo (≥ 80% instruction coverage enforced) |
| CI/CD | GitHub Actions |

---

## Project Structure

```
fcs-interview-code-assignment-main/
├── .github/
│   └── workflows/
│       └── ci.yml                         # CI pipeline (build, test, coverage)
├── assignment/
│   └── CODE_ASSIGNMENT.md                 # Original task specification
├── case-study/
│   └── CASE_STUDY.md                      # Scenario answers
├── java-assignment/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fulfilment/application/monolith/
│   │   │   │   ├── location/              # Location gateway
│   │   │   │   ├── products/              # Product CRUD
│   │   │   │   ├── stores/                # Store CRUD + event observer pattern
│   │   │   │   └── warehouses/
│   │   │   │       ├── adapters/
│   │   │   │       │   ├── database/      # JPA entities + repositories
│   │   │   │       │   └── restapi/       # REST controllers
│   │   │   │       ├── domain/
│   │   │   │       │   ├── models/        # Domain models (Warehouse, Location, …)
│   │   │   │       │   ├── ports/         # Input & output port interfaces
│   │   │   │       │   ├── usecases/      # Business orchestration
│   │   │   │       │   └── validator/     # WarehouseValidator, FulfillmentValidator
│   │   │   │       └── fulfillment/
│   │   │   │           └── event/         # CDI domain events
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/com/fulfilment/application/monolith/
│   │           ├── stores/                # StoreEventHandlerTest
│   │           └── warehouses/
│   │               └── domain/
│   │                   ├── usecases/      # Use case tests
│   │                   └── validator/     # Validator unit tests
│   ├── QUESTIONS.md                       # Architecture Q&A
│   └── pom.xml
└── README.md                              # ← You are here
```

---

## Getting Started

### Prerequisites

- **JDK 17** (Temurin / OpenJDK)
- **PostgreSQL 16** running locally
- **Maven** (the `./mvnw` wrapper is included)

### Local Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/omshukla684/fcs-warehouse-assignment.git
   cd fcs-warehouse-assignment/java-assignment
   ```

2. **Create the database:**
   ```sql
   CREATE DATABASE quarkus_test;
   ```

3. **Configure your credentials** — edit `src/main/resources/application.properties`:
   ```properties
   quarkus.datasource.username=postgres
   quarkus.datasource.password=your_password
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_test
   ```

### Running the Application

```bash
# Start in development mode (hot-reload enabled)
./mvnw quarkus:dev

# Application available at:
#   http://localhost:8080
#   http://localhost:8080/q/swagger-ui/   ← Interactive API docs
```

---

## API Reference

### Warehouse API

All warehouse endpoints are defined by `warehouse-openapi.yaml` and exposed under `/warehouse`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/warehouse` | List all active warehouses |
| `POST` | `/warehouse` | Create a new warehouse |
| `GET` | `/warehouse/{id}` | Get warehouse by business-unit code |
| `PUT` | `/warehouse/{id}` | Replace (archive + recreate) warehouse |
| `DELETE` | `/warehouse/{id}` | Archive a warehouse |

**Create/Replace Warehouse Request Body:**
```json
{
  "businessUnitCode": "MWH.001",
  "location": "AMSTERDAM-001",
  "capacity": 100,
  "stock": 45
}
```

### Store API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/store` | List all stores |
| `POST` | `/store` | Create a store |
| `GET` | `/store/{id}` | Get store by ID |
| `PUT` | `/store/{id}` | Full update |
| `PATCH` | `/store/{id}` | Partial update |
| `DELETE` | `/store/{id}` | Delete a store |

> **Note:** All mutating store operations use CDI events observed with `@TransactionPhase.AFTER_SUCCESS` to guarantee the legacy system (`LegacyStoreManagerGateway`) is only notified after the database transaction has committed.

### Fulfillment API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/warehouse/{warehouseCode}/store/{storeId}/product/{productId}` | Associate a product with a warehouse-store pair |

---

## Business Rules

### Warehouse Rules
| Rule | Description |
|------|-------------|
| **Unique code** | No two active warehouses may share the same `businessUnitCode` |
| **Valid location** | `location` must exist in the location catalog |
| **Max warehouses/location** | Active warehouse count ≤ `location.maxNumberOfWarehouses` |
| **Max total capacity** | Sum of capacities at location ≤ `location.maxCapacity` |
| **Stock coherence** | `stock` ≤ `capacity` |
| **Replace – capacity** | New warehouse `capacity` ≥ old warehouse `stock` |
| **Replace – stock** | New warehouse `stock` must equal old warehouse `stock` |

### Fulfillment Rules
| Rule | Description |
|------|-------------|
| **Max warehouses/store** | Max 3 distinct warehouses per store |
| **Max warehouses/product/store** | Max 2 warehouses serving the same product in one store |
| **Max products/warehouse** | Max 5 distinct product types per warehouse |

---

## Testing & Coverage

Run all tests with coverage report:

```bash
./mvnw verify
```

The build **enforces ≥ 80% instruction coverage** via JaCoCo. If coverage drops below this threshold, the build fails.

Coverage report is generated at: `target/site/jacoco/index.html`

**Test structure (67 tests, 0 failures):**
```
tests/
├── warehouses/domain/usecases/
│   ├── CreateWarehouseUseCaseTest                 (7 tests)
│   ├── ReplaceWarehouseUseCaseTest                (6 tests)
│   ├── ArchiveWarehouseUseCaseTest                (3 tests)
│   └── AssociateWarehouseProductStoreUseCaseTest (6 tests)
├── warehouses/domain/validator/
│   ├── WarehouseValidatorTest                     (21 tests)
│   └── FulfillmentValidatorTest                   (11 tests)
├── stores/
│   └── StoreEventHandlerTest                      (2 tests)
├── location/
│   └── LocationGatewayTest                        (10 tests)
└── products/
    └── ProductEndpointTest                        (1 test)
```


---

## CI/CD Pipeline

GitHub Actions runs on every push/PR to `main`:

1. ✅ **Checkout & JDK 17 setup**
2. ✅ **PostgreSQL 15 service** (health-checked)
3. ✅ **Maven verify** (compile + test + JaCoCo coverage check)
4. ✅ **Upload coverage report** as artifact (retained 14 days)
5. ✅ **Publish test results** as PR comment

Pipeline definition: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

---

## Design Decisions

### Hexagonal Architecture
All business logic lives in the `domain` layer and depends only on interfaces (ports). Infrastructure details (JPA, REST, external services) are in the adapters layer, making the domain independently testable without a running database.

### Validation Separation (Task 3 & 4)
Validation logic is extracted into dedicated classes:
- `WarehouseValidator` — warehouse creation, replacement, and stock rules
- `FulfillmentValidator` — fulfillment association constraints

This keeps use cases focused on orchestration and makes each validation rule independently testable.

### Store Event Observer Pattern (Task 6)
`StoreResource` fires CDI events (`StoreCreatedEvent`, `StoreUpdatedEvent`) instead of calling `LegacyStoreManagerGateway` directly. `StoreEventHandler` observes these events with `@TransactionPhase.AFTER_SUCCESS`, guaranteeing that the legacy system is only notified after the database transaction commits — eliminating ghost notifications.

### Fulfillment Hexagonal Package
The fulfillment feature follows a proper hexagonal structure:
- **Input port**: `AssociateFulfillmentOperation` (interface the REST adapter calls)
- **Output port**: `FulfillmentStore` (interface the use case calls for persistence)
- **Domain event**: `FulfillmentCreatedEvent` (fired post-association)
- **Infrastructure adapter**: `FulfillmentRepository` (implements `FulfillmentStore`)
