# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The codebase uses two different DB access patterns:

1. Active Record pattern (PanacheEntity) — used by Store and Product.
   These entities inherit from PanacheEntity and expose static persistence methods
   directly on the entity class (e.g., Store.listAll(), entity.persist()).

2. Repository pattern (PanacheRepository) — used by Warehouse (WarehouseRepository).
   The entity (DbWarehouse) is a plain JPA entity; all DB operations go through a
   dedicated repository class that implements a domain port (WarehouseStore).

If I were maintaining this codebase, I would standardize on the Repository pattern
(option 2) for the following reasons:

- Separation of Concerns: Active Record conflates domain/data concerns. The Repository
  pattern keeps the entity as a pure data structure and the repository as the data-access
  concern, which aligns with the hexagonal architecture already in place for Warehouses.

- Testability: With the Repository behind a port interface, we can mock the entire
  persistence layer in unit tests without needing a database. Active Record entities
  are tightly coupled to the DB and require integration tests or extensive mocking setup.

- Consistency with Hexagonal Architecture: The Warehouse side already demonstrates the
  clean separation of domain model, domain port, and infrastructure adapter. Refactoring
  Store and Product to follow the same pattern would make the codebase uniform and easier
  to onboard new developers.

- Flexibility: A repository is easy to swap (e.g., switch from Panache to Spring Data,
  or to an in-memory store for testing), whereas Active Record makes that swap much more
  invasive.

The refactoring effort is straightforward: introduce StoreRepository and ProductRepository
implementing a port interface, move all static calls to the repository, and remove the
PanacheEntity inheritance from Store and Product in favor of plain PanacheRepository.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Approach A: OpenAPI-first (code generated from YAML spec) — used by Warehouse
  Pros:
  - Single source of truth: the spec IS the contract. Clients, documentation,
    and server stubs all derive from it.
  - Design-first: forces teams to agree on the API contract before writing any code,
    reducing miscommunication and late-stage API changes.
  - Auto-generated documentation: Swagger UI and ReDoc work out of the box.
  - Schema validation is enforced at the framework level.
  - Easier for API-gateway integration, mocking, and consumer-driven contract testing.
  Cons:
  - Adds a build step (code generation); generated code can be verbose and harder to
    navigate for unfamiliar developers.
  - Changes to the spec require regenerating and reconciling generated code.
  - Less natural when iterating rapidly in early development stages.

Approach B: Code-first (annotations on resource classes) — used by Store and Product
  Pros:
  - Faster to bootstrap — just annotate your JAX-RS resource and go.
  - Developer experience is more natural; no context switching between YAML and Java.
  - Works well for small, internal, or stable APIs.
  Cons:
  - The API spec is derived from the code (often via Quarkus SmallRye OpenAPI at runtime),
    which means it's harder to share or validate the contract upfront.
  - Teams can inadvertently break the API surface without noticing until runtime.
  - Keeping documentation synchronized requires discipline.

My choice: OpenAPI-first (Approach A) for all new endpoints.

In a production system serving external consumers or multiple teams, the spec-first
approach pays dividends quickly: consistent contracts, generated SDK stubs for clients,
and clear versioning. I would gradually migrate the Product and Store endpoints to
OpenAPI-first, using the existing code to derive a baseline YAML spec and then making
it the authoritative document going forward.

For internal admin APIs or rapid prototypes, code-first is perfectly acceptable as a
starting point, but it should be promoted to spec-first once the API stabilizes.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Testing Pyramid — my prioritization:

1. Unit Tests (highest priority, most tests)
   Target: Domain use cases (business logic), gateways, and utility classes.
   Why: They are fast, isolated (mocked dependencies), and directly verify business
   rules. For this project the use cases encapsulate all critical business logic
   (capacity checks, stock matching, archive logic), so this is where most test effort
   goes. Mocking WarehouseStore and LocationResolver keeps them dependency-free.
   Tools: JUnit 5 + Mockito.

2. Integration Tests (medium priority)
   Target: Repository implementations and REST endpoints.
   Why: They verify the DB mapping, Panache queries, and full HTTP request-response cycles.
   Quarkus @QuarkusTest and Dev Services make this easy — a real (containerized) DB is
   started automatically.
   Strategy: Cover the main happy path and key error paths (404, 400). Don't duplicate
   every unit test scenario here.
   Tools: @QuarkusTest + REST-Assured + Quarkus Dev Services (auto-Postgres).

3. Contract Tests (where applicable)
   Target: OpenAPI-generated interfaces.
   Why: Ensure the implementation matches the contract and that generated code stays in sync.

4. End-to-End Tests (low count, high value)
   Target: Critical user journeys (e.g., create → replace → archive warehouse).
   Why: Provides confidence in the full stack but is slow and flaky; keep these minimal.

Ensuring coverage stays effective over time:
- Gate on CI: JaCoCo check goal enforces ≥80% instruction coverage on every PR.
  CI pipeline fails if coverage drops below threshold.
- Coverage reports uploaded as CI artifacts so reviewers can inspect uncovered lines.
- Test review in PR process: code reviewers check that new business logic includes
  corresponding unit tests.
- Mutation testing (optional): Tools like PIT can reveal tests that pass but don't
  actually assert the right behavior — valuable for critical business-rule code.
- Avoid testing for coverage's sake: focus on testing behavior and edge cases,
  not just hitting percentage targets.
```