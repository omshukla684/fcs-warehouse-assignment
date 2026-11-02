# Warehouse Colocation Management System

[![CI - Build, Test & Coverage](https://github.com/omshukla684/fcs-warehouse-assignment/actions/workflows/ci.yml/badge.svg)](https://github.com/omshukla684/fcs-warehouse-assignment/actions/workflows/ci.yml)

A Quarkus-based monolith for managing Warehouses, Stores, Products, and their fulfillment relationships.

---

## Assignment Status

| Task | Status |
|---|---|
| Task 1: `LocationGateway.resolveByIdentifier` | Completed |
| Task 2: `StoreResource` post-commit legacy calls | Completed (CDI `AFTER_SUCCESS` events) |
| Task 3: Warehouse CRUD + all business validations | Completed |
| Task 3: Warehouse API endpoints | Completed |
| BONUS: Warehouse-Product-Store fulfillment association | Completed |
| Unit Tests (80%+ coverage via JaCoCo) | Completed |
| CI/CD Pipeline (GitHub Actions) | Completed |
| QUESTIONS.md answered | Completed |
| CASE_STUDY.md answered | Completed |

---

## Architecture Overview

The project follows a **Hexagonal Architecture** for the Warehouse domain:

```
REST Layer (adapters/restapi)
    ↓
Domain Use Cases (domain/usecases)
    ↓ (via port interfaces)
Database Layer (adapters/database)
```

The Store and Product domains use the simpler **Active Record** (PanacheEntity) pattern.

---

## Key Design Decisions

### Task 2: Post-Commit Legacy Gateway Calls
The `StoreResource` now fires CDI events (`StoreCreatedEvent`, `StoreUpdatedEvent`) within the transaction. The `StoreEventHandler` observes these events with `@TransactionPhase.AFTER_SUCCESS`, guaranteeing the legacy system is only notified **after** the database commit succeeds.

### Task 3: Business Validations
All warehouse business rules are enforced in the use cases:
1. **BusinessUnitCode Uniqueness** — no duplicate active warehouse codes
2. **Location Validation** — must match a known location in `LocationGateway`
3. **Max Warehouses per Location** — enforced per `Location.maxNumberOfWarehouses`
4. **Total Capacity Limit** — sum of all warehouse capacities at a location ≤ `Location.maxCapacity`
5. **Stock ≤ Capacity** — warehouse stock cannot exceed its own capacity
6. **Replace: Capacity Accommodation** — new warehouse capacity ≥ old warehouse stock
7. **Replace: Stock Matching** — new warehouse stock must equal old warehouse stock

### BONUS: Fulfillment Association
`POST /warehouse/{warehouseCode}/store/{storeIdentifier}/product/{productIdentifier}` creates the association, enforcing:
- Max 2 warehouses per product per store
- Max 3 warehouses per store
- Max 5 product types per warehouse

---

## Requirements

- **JDK 17+**
- **Docker** (for running PostgreSQL automatically via Quarkus Dev Services in `dev`/`test` mode)
- **Maven** (wrapper `./mvnw` included)

---

## Running the Application

### Development Mode (recommended — auto-starts PostgreSQL via Docker)

```sh
cd java-assignment
./mvnw quarkus:dev
```

Access the application at <http://localhost:8080>

### Running Unit Tests with Coverage

```sh
cd java-assignment
./mvnw verify
```

JaCoCo coverage report: `target/site/jacoco/index.html`
Coverage is enforced at **≥ 80%** — the build fails if coverage drops below this.

### Production Mode

Start PostgreSQL:
```sh
docker run -it --rm --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

Build and run:
```sh
./mvnw package -DskipTests
java -jar ./target/quarkus-app/quarkus-run.jar
```

---

## API Endpoints

### Warehouse API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/warehouse` | List all active warehouses |
| `POST` | `/warehouse` | Create a new warehouse |
| `GET` | `/warehouse/{businessUnitCode}` | Get warehouse by code |
| `DELETE` | `/warehouse/{businessUnitCode}` | Archive a warehouse |
| `POST` | `/warehouse/{businessUnitCode}/replacement` | Replace a warehouse |

### BONUS: Fulfillment API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/warehouse/{wCode}/store/{storeId}/product/{productId}` | Associate warehouse→product→store |

### Store API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/store` | List all stores |
| `POST` | `/store` | Create a store |
| `GET` | `/store/{id}` | Get store by ID |
| `PUT` | `/store/{id}` | Update a store |
| `PATCH` | `/store/{id}` | Partially update a store |
| `DELETE` | `/store/{id}` | Delete a store |

### Product API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/product` | List all products |
| `POST` | `/product` | Create a product |
| `GET` | `/product/{id}` | Get product by ID |
| `DELETE` | `/product/{id}` | Delete a product |

---

## CI/CD Pipeline

GitHub Actions workflow at `.github/workflows/ci.yml`:
- Triggered on push/PR to `main`, `master`, `develop`
- Sets up JDK 17 with Maven caching
- Runs `./mvnw verify` (unit tests + JaCoCo coverage check at ≥80%)
- Uploads coverage report as a CI artifact
- Publishes test results as a PR comment
- Optional native image build on `main` branch pushes

---

## Troubleshooting

- **IntelliJ IDEA**: Add `target/.../jaxrs` and `target/generated-sources` as "Generated Sources Root" to resolve the OpenAPI-generated `WarehouseResource` interface.
- **Port conflicts**: If port 8080 is in use, set `quarkus.http.port=8081` in `application.properties`.
- **Docker not running**: Dev Services require Docker. Start Docker Desktop before running `quarkus:dev` or tests.