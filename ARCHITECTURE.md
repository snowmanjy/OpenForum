# OpenForum - Architecture & Design Spec

## Architecture Principles

### We Follow Clean Architecture

- Hexagonal Architecture / DDD
- Functional programming principles (map/flatMap, immutability, Optional, Either)
- Clean code (SRP, no long methods, no primitive obsession)
- TDD supportability (easy to mock, pure functions)
- No Spring annotations in domain; keep side effects in adapters.
- Minimize if/else; use pattern matching, polymorphism, or strategy.

```
Controllers → Use Cases (Services) → Domain Entities → Repositories
```

- Controllers should be thin - only handle HTTP concerns
- Services contain business logic
- Domain entities are rich, not anemic
- Repositories abstract data access

### Design Philosophy

- **Immutability First**: Use `final` fields, Java Records, and immutable collections
- **Fail Fast**: Validate at boundaries, use Optional to make null handling explicit
- **Functional Core, Imperative Shell**: Pure functions for business logic, side effects at edges
- **Tell, Don't Ask**: Objects should do things, not expose their state for others to manipulate

## 1\. Project Overview & Constraints

**System Goal:** Build the high-performance, DDD based, headless Engine for a multi-tenant community platform. Scope: This project contains ONLY the domain logic, data storage, and API. It does not contain specific SaaS business logic (billing, legacy adapters).

### 1.1 Tech Stack (Strict)

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.4+
- **Build Tool:** Maven (Multi-module)
- **Database:** PostgreSQL 16
- **ORM:** Hibernate 6.x (Must use native `JSONB` support via `@JdbcTypeCode(SqlTypes.JSON)`)
- **Search:** Elasticsearch (later phase) or Postgres Full Text Search (initial phase)
- **API Style:** REST (primary), following CloudEvents for domain events.
- **Messaging: Kafka (via Debezium/Outbox Pattern)

### 1.2 Coding Style Constraints

- **Functional Paradigm:** Prefer Java Streams and functional interfaces over imperative loops.
  - **Bad:** `for (Event e : events) { save(e); }`
  - **Good:** `events.stream().map(this::toEntity).forEach(repo::save);`
- **Immutability:** Use `final` keywords, Java Records, and `List.of()`/`Map.of()` factories wherever possible.
- **Layering:** Controllers MUST call Services. Services MUST call Repositories. Never skip a layer.

### 1.4 Factory & Creation Patterns

- **Complex Validation:** Use a dedicated Factory class (e.g., `ThreadFactory`) when creation logic involves complex validation or invariants (e.g., checking for required keys in a metadata map).
  - **Do not** put complex validation logic inside the Aggregate constructor or static factory methods on the Aggregate itself.
- **Hidden Constructors:** Hide public constructors of Aggregates (`private` or `protected`).
  - Use **Factory classes** for creating new instances with invariant enforcement.
  - Use a static `reconstitute` method on the Aggregate for Infrastructure/Repositories to restore state from the database without triggering domain validation logic.
- **Builder pattern:** use the builder pattern instead of constructors with lots of parameters.

### 1.3 Architecture Pattern: Clean Architecture / DDD / TDD

We strictly separate the **Domain** from the **Infrastructure**.

- **Rule 1:** The `forum-domain-core` module must have **zero dependencies** on Spring, Hibernate, or Web libraries. It is pure Java.
- **Rule 2:** Dependencies flow **inwards**: `Infrastructure` -\> `Application` -\> `Domain`.

-----

## 2\. Module Structure

The project must be organized into the following Maven modules:

```text
root/
├── forum-domain-core/       # [PURE JAVA] Aggregates, Value Objects, Domain Events, Repository Interfaces
├── forum-application/       # [SPRING] Transactional Services
├── forum-infra-jpa/         # [SPRING DATA] Postgres & Outbox Pattern
├── forum-interface-rest/    # [SPRING WEB] Public API for SaaS UI
├── forum-interface-admin/   # [SPRING WEB] Internal/Bulk API for SaaS Backend
├── forum-interface-ai/      # [SPRING] AI "Member" Integration (LLM Client & Listeners)
└── forum-boot/              # [SPRING BOOT] Main entry point, configuration assembly
```

### Constraint: Layered Isolation

#### Strict Layering: Controllers (Interface Layer) MUST NEVER depend on Repositories (Infrastructure Layer)

#### Mandatory Service Layer: All Controller logic must delegate to an Application Service in forum-application

#### Responsibility: The Application Service is the Transaction Boundary (@Transactional) and is responsible for

- Loading Aggregates via Repositories.

- Invoking Domain Logic on the Aggregate.

- Saving state changes.

-----

## 3\. Core Domain Model (forum-domain-core)

### 3.1 Aggregates & Entities

**1. Thread (Aggregate Root)**

- **ID:** UUID
- **TenantID:** String (Isolation key)
- **AuthorID:** UUID (Reference to Member)
- **Title:** String
- **Status:** Enum (OPEN, CLOSED, ARCHIVED)
- **Metadata:** `Map<String, Object>` (Crucial: This stores vertical-specific data like "deal\_irr", "pet\_breed", etc. Persisted as JSONB).
- **Posts:** `List<Post>` (Note: For performance, infrastructure implementation may load this lazily or strictly manage the collection size).

**2. Post** (Entity within Thread Aggregate)

- **ID:** UUID
- **ThreadID:** UUID (Parent Reference)
- **AuthorID:** UUID (Reference to Member)
- **Content:** String (Markdown supported)
- **Version:** Long (Optimistic Locking for offline-sync conflict resolution)
- **ReplyToPostID:** UUID (Optional, for nested threading)
- **Metadata:** `Map<String, Object>` (Stores: Reactions ({"likes": 10}), AI Analysis ({"sentiment": "negative"}), etc. Persisted as JSONB).

**3. Member (User Profile)**

- **ID:** UUID
- **ExternalID:** String (The ID from the Parent App/Auth Provider)
- **IsBot:** Boolean (True for AI agents)
- **Reputation:** Integer

**4. Tenant (Aggregate Root)**

- **ID:** String (Provided by SaaS Platform)
- **Config:** `Map<String, Object>` (Crucial: This stores runtime flags: isInviteOnly, aiEnabled, allowedVerticals, etc. Persisted as JSONB).

### 3.2 Domain Events Lifecycle (The Outbox Pattern)

- `ThreadCreatedEvent`
- `PostCreatedEvent`
- `ThreadClosedEvent`

To ensure data consistency and enable the Data Lake:

Accumulation: Aggregates MUST NOT publish events directly. They must maintain a `private List<Object> domainEvents and expose a pollEvents() method.

#### Constraint: State-Carried Events & Outbox Pattern

- **Philosophy:** Domain Events are the source of truth for future Reporting Services (Data Lake).
- **Payloads:** Events must carry the full state of the change, not just the ID.
- **Example:** `ThreadUpdatedEvent` must contain both `oldStatus` and `newStatus`, plus the current metadata snapshot.
- **Persistence:** We implement the Transactional Outbox Pattern.

When the `ThreadRepository` saves an aggregate, it must strictly also save an `OutboxEvent` entity in the same ACID transaction.

**Table Schema (`outbox_events`):**

- `id`: UUID
- `aggregate_id`: UUID
- `type`: String (e.g., "ThreadCreated")
- `payload`: JSONB (The actual event data)
- `created_at`: Timestamp

**Why:** This allows us to use a CDC connector (Debezium) on the `outbox_events` table later to populate our Data Lake without locking the main entity tables.

#### Infrastructure: Async Messaging

- **Broker:** Kafka
- **Producer Strategy:** Transactional Outbox (Postgres -> Debezium -> Kafka).
- **Topic Strategy:**
  - **Topic:** `forum-events-v1`
  - **Key:** `tenant_id` (Ensures all events for a tenant are ordered) OR `thread_id` (finer granularity).
  - **Payload:** JSON (CloudEvents standard recommended).

### 3.3 Domain Event Lifecycle (Strict)

To guarantee the Transactional Outbox Pattern, we adhere to the following rules in the Domain Core:

#### 1. Accumulation, Not Publication

- Aggregates (e.g., `Thread`) **MUST NOT** call external publishers or static buses.
- Aggregates must maintain a private list of transient events: `List<Object> domainEvents`.
- Aggregates must expose a method `List<Object> pollEvents()` that clears and returns this list.

#### 2. Repository Contract

- The `ThreadRepository.save(Thread t)` interface implies a dual responsibility:
    1. Persist the Thread state (UPDATE/INSERT).
    2. **Atomic Side Effect:** Extract events via `t.pollEvents()` and persist them to the `outbox_events` table in the same transaction.

#### 3. No Side Effects in Domain

The Domain Core code knows nothing about the database or the outbox table. It simply queues the event object in the list
-----

## 4\. Key Technical Strategies

### 4.1 Authentication: "Trusted Parent" (Multipass)

- **Philosophy:** We do not own user credentials. We trust the the SaaS Platform.
- **Mechanism:** JWT Passthrough.
- **Implementation:**
  - `forum-interface-rest` contains a Security Filter.
  - Validate JWT signature against configured Public Key / JWKS.
  - **JIT Provisioning:** If the `sub` (Subject ID) in the JWT does not exist in the `Member` table, create a `Member` record immediately using the JWT claims (email, name).
  - **No** separate login endpoint.

### 4.2 Data Strategy: Vertical Templates via JSONB

- To support different verticals (Real Estate, Pets, TCG) without schema migration, we use a hybrid schema.
- Common fields (`title`, `created_at`) are typed SQL columns.
- Vertical-specific fields (`card_rarity`, `apartment_class`) live in the `Thread.metadata` map.
- **Hibernate Mapping:** Use `@JdbcTypeCode(SqlTypes.JSON)` on the entity field.

### 4.3 AI Strategy: "The Member Persona"

- **Concept:** AI is not a backend ghost; it is a visible "User" with `isBot=true`.
- **Flow:**
    1. `PostCreatedEvent` is published.
    2. `forum-interface-ai` listens to the event asynchronously.
    3. Service checks tenant config: "Is AI enabled for this category?"
    4. Service generates response via LLM.
    5. Service calls `replyToThread` acting as the AI User.
    6. **Safety:** The listener MUST ignore posts where `author.isBot() == true` to prevent infinite loops.

### 4.4 Bulk Ingestion (The SaaS Bridge)

- To support the SaaS project's migration tools, `forum-interface-admin` must expose high-throughput endpoints.
- **Endpoint:** `POST /admin/v1/bulk/threads`
- **Behavior:**
  - Accepts a batch of Thread DTOs (e.g., 100-1000 items).
  - Persists them efficiently.
  - **Silent Mode:** Does NOT generate `ThreadCreated` notification events (to prevent spamming users during migration imports). It MAY generate "Audit" events for the Data Lake.

## 5\. The "SaaS Contract" (How the wrapper Saas project talks to us)

### 5.1 Multi-Tenancy

The Core does not manage "Subscriptions."

It trusts the TenantID passed in the header X-Tenant-ID.

It trusts the User passed in the JWT.

### 5.2 Bulk Ingestion (Replacing Migration Module)

To support the SaaS layer's migration tools, the Core must expose a specialized Bulk Interface in forum-interface-admin.

Requirement: High-throughput writing without triggering downstream notification storms (e.g., "Don't email 10k users during an import").

Method: POST /admin/v1/bulk/threads

Accepts: List of Thread DTOs.

Behavior: Persists data but suppresses ThreadCreated notification events (or marks them as "Historical")
-----

## 7\. Implementation Phases (For Agent Execution)

**Phase 1: The Skeleton**

- Set up Maven modules.
- Configure Testcontainers for PostgreSQL integration testing.

**Phase 2: Domain Core**

- Implement `Thread` and `Post` aggregates (Pure Java).
- Define `ThreadRepository` interface.

**Phase 3: Infrastructure (JPA)**

- Implement `ThreadRepository` using Spring Data JPA.
- Configure JSONB mapping for `Thread.metadata`.
- Write integration tests to verify JSONB persistence.

**Phase 4: REST API & Auth**

- Implement `ThreadController`.
- Implement `JwtAuthenticationFilter` (The "Trusted Parent" logic).

**Phase 5: Subscriptions (Core)**

**Goal:** Allow users to subscribe to threads and efficiently query subscribers for notification dispatch.

**1. Domain Model (forum-domain-core)**

- **New Aggregate:** `Subscription`
  - `id`: UUID
  - `tenantId`: String
  - `userId`: UUID
  - `targetId`: UUID (The Thread ID)
  - `targetType`: Enum (THREAD)
  - `createdAt`: LocalDateTime
- **Business Rule:** A user can only subscribe to a thread once.

### 5.2. Infrastructure

- **Table:** `subscriptions`
- **Constraints:**
  - PK: `id`
  - Unique: `(user_id, target_id)`
- **Indices:**
  - `idx_subscription_target`: `(target_id)` (For notification dispatch)
  - `idx_subscription_user`: `(user_id)` (For "My Subscriptions" list)

### 5.3. Application Service

- **Service:** `SubscriptionService`
- **Methods:**
  - `subscribe(tenantId, userId, threadId)`: Idempotent.
  - `unsubscribe(tenantId, userId, threadId)`: Idempotent.
  - `getSubscribers(threadId)`: Returns list of User IDs.
  - `getSubscriptionsForUser(tenantId, userId, page)`: Returns `SubscriptionWithThreadDto` (Aggregate Stitching: Fetches Thread titles).

### 5.4. API

- **Public:**
  - `POST /api/v1/threads/{threadId}/subscriptions`: Subscribe.
  - `DELETE /api/v1/threads/{threadId}/subscriptions`: Unsubscribe.
  - `GET /api/v1/threads/{threadId}/subscribers`: Get all subscribers for a thread.
  - `GET /api/v1/subscriptions`: List my subscriptions (Paginated).
    - Response: `{ data: [{ threadId, threadTitle, subscribedAt }], page, total }`

**Phase 2.2: Categories & Search**

**Goal:** Organize threads into categories and provide full-text search capabilities.

**1. Categories (Domain & Infra)**

- **Aggregate:** `Category`
  - `id`: UUID
  - `tenantId`: String
  - `name`: String
  - `slug`: String (Unique per tenant)
  - `description`: String
  - `isReadOnly`: Boolean
- **Constraints:** Unique Index on `(tenant_id, slug)`.
- **API:** `CategoryController` (CRUD).

**2. Thread Linking**

- **Refactor:** Add `categoryId` (UUID) to `Thread` aggregate.
- **Migration:** Add `category_id` column to `threads` table.

**3. Full-Text Search (Postgres)**

- **Strategy:** Use PostgreSQL native Full-Text Search (FTS).
- **Schema:** Add `search_vector` (tsvector) column to `threads`.
- **Index:** GIN Index on `search_vector`.
- **Update Logic:** Generated Column or Trigger to update `search_vector` from `title` + `metadata`.
- **Query:** `SELECT * FROM threads WHERE tenant_id = :tenantId AND search_vector @@ websearch_to_tsquery('english', :query)`

**4. Subscription Logic Update**

- **DTO:** `SubscriptionDto` (Generic: `targetId`, `targetType`, `title`, `subscribedAt`).
- **Service:** `SubscriptionService` handles both `THREAD` and `CATEGORY` targets.
- **Validation:** Ensure `TargetType.CATEGORY` subscriptions validate against `CategoryRepository`.
- **API:**
  - `POST /api/v1/categories/{categoryId}/subscriptions`
  - `DELETE /api/v1/categories/{categoryId}/subscriptions`
  - `GET /api/v1/subscriptions` returns mixed list of threads and categories.

## 8\. Agent Implementation Rules (For Antigravity)

**Rule 1: No Dual Writes**
Never write code that saves to the DB and then sends to Kafka/API in the same method. Always use the Outbox table.

**Rule 2: Vertical Flexibility**
Do not create hardcoded Java classes for Vertical data (e.g., no PetThread.java). Always use the Thread.metadata Map/JSONB strategy.

**Rule 3: Test Separation**

- forum-domain-core: Unit tests only (No Spring, No DB).
- forum-infra-jpa: Integration tests with Testcontainers (Postgres).
