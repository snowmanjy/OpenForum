# OpenForum

**OpenForum** is a DDD-first, headless, multi-tenant forum engine written in Java 21 and Spring Boot 3.

It is designed as the **core engine** behind a SaaS community product and vertical-specific forums
(apartment syndication, TCG retailers, local pet lost & found, etc.), with:

- Clean / Hexagonal architecture  
- Pure Java domain model (no Spring dependencies)  
- PostgreSQL 16 + JSONB for vertical-specific metadata  
- Transactional outbox for domain events (ready for Kafka / Debezium)  
- REST interfaces for public/SPA clients, admin tools, and AI integrations  

> **Status:** Early core implementation. Domain, application, JPA infrastructure and basic REST layer are in place.  
> SaaS features (billing, templates, SEO/ads layer, etc.) are intentionally out of scope for this repo.

---

## Architecture Overview

The architecture is documented in detail in [`ARCHITECTURE.md`](./ARCHITECTURE.md).  
Short version:

- **Clean / Hexagonal / DDD**  
- **Functional Core, Imperative Shell**  
- **Domain is pure Java** – no Spring, no JPA, no Web annotations in `forum-domain-core`.  
- **Outbox pattern** for domain events, ready to be streamed to Kafka.

High-level flow:

```text
Controllers (forum-interface-*) 
    → Application Services (forum-application) 
        → Domain Aggregates (forum-domain-core) 
            → Repositories (interfaces)
                → JPA / Postgres (forum-infra-jpa)
```

### Modules

This is a multi-module Maven project:

```text
root/
├── forum-domain-core     # [PURE JAVA] Aggregates, VOs, Domain Events, Repository interfaces
├── forum-application     # [SPRING] Transactional services / use cases
├── forum-infra-jpa       # [SPRING DATA JPA] Postgres, Flyway, Outbox, Testcontainers
├── forum-interface-rest  # [SPRING WEB] Public REST API (headless forum engine)
├── forum-interface-admin # [SPRING WEB] Admin / bulk / internal APIs (future)
├── forum-interface-ai    # [SPRING] AI "member" / LLM integration (future)
└── forum-boot            # [SPRING BOOT] Main application & configuration assembly
```

Key tech stack:

- **Language:** Java 21  
- **Framework:** Spring Boot 3.4.x  
- **Database:** PostgreSQL 16  
- **ORM:** Spring Data JPA + Hibernate, with `JSONB` columns  
- **Migrations:** Flyway  
- **Testing:** JUnit 5, AssertJ, Testcontainers (Postgres)  
- **Messaging (planned):** Kafka via transactional outbox + Debezium  
- **AI (planned):** Spring AI (BOM imported in parent POM)  

---

## Core Domain Model (forum-domain-core)

The `forum-domain-core` module contains the pure domain model.

### Aggregates

- **Thread**
  - `id` (UUID)  
  - `tenantId` (String)  
  - `authorId` (UUID, member)  
  - `title` (String)  
  - `status` (OPEN, CLOSED, ARCHIVED, …)  
  - `metadata` (Map → JSONB)  
    - Used for **vertical-specific attributes** (e.g. `deal_irr`, `pet_breed`, `last_seen_location`)  
  - Maintains domain events (e.g. `ThreadCreatedEvent`, `ThreadClosedEvent`)

- **Post**
  - `id` (UUID)  
  - `threadId` (UUID)  
  - `authorId` (UUID)  
  - `content` (Markdown-friendly)  
  - `version` (for optimistic locking / offline edits)  
  - `replyToPostId` (optional, for reply chains)  
  - `metadata` (Map → JSONB: reactions, AI analysis, etc.)

- **Member**
  - `id` (UUID)  
  - `externalId` (String, ID from parent app / auth provider)  
  - `isBot` (boolean, used for AI agents)  
  - `reputation` (int)

- **Tenant**
  - `id` (String, provided by SaaS platform)  
  - `config` (Map → JSONB)  
    - Flags like `isInviteOnly`, `aiEnabled`, `allowedVerticals`, etc.

> For more details, see the **"Core Domain Model"** section in `ARCHITECTURE.md`.

### Domain Events & Outbox

Domain aggregates **accumulate** events; they do not publish directly.

- Aggregates keep a private `List<DomainEvent>`.  
- They expose `pollEvents()` used by repositories.  
- On `save(aggregate)` the repository:
  1. Persists the aggregate.  
  2. Persists corresponding **OutboxEvent** rows in the same transaction.  

Outbox table (simplified):

- `id` (UUID)  
- `aggregate_id` (UUID)  
- `type` (String, e.g. `"ThreadCreated"`)  
- `payload` (JSONB, full event state)  
- `created_at` (timestamp)  

Later, Debezium can stream these to Kafka for analytics / search / data lake.

---

## Getting Started

### Prerequisites

- Java 21  
- Maven 3.9+  
- Docker + Docker Compose  
- (Optional) `curl` / HTTP client or Postman  

### 1. Clone the repo

```bash
git clone https://github.com/snowmanjy/OpenForum.git
cd OpenForum
```

### 2. Run with Docker Compose

This is the easiest way to spin up Postgres + the app:

```bash
docker compose up --build
```

This will:

- Start **Postgres 16** with:
  - DB: `openforum_db`
  - User: `openforum`
  - Password: `password`
- Build and run the `openforum` service from `forum-boot/Dockerfile`
- Expose the app on: `http://localhost:8080`
- Expose Postgres on: `localhost:5432`

> Database settings are configured via environment variables in `docker-compose.yml`.

### 3. Run locally with Maven (no Docker)

If you already have a local Postgres running, you can configure:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/openforum_db
export SPRING_DATASOURCE_USERNAME=openforum
export SPRING_DATASOURCE_PASSWORD=password
```

Then build & run:

```bash
./mvnw clean install
./mvnw spring-boot:run -pl forum-boot
```

The app should be available at: `http://localhost:8080`.

### 4. Health check

If the Spring Boot actuator is enabled:

```bash
curl http://localhost:8080/actuator/health
```

---

## API Overview

The `forum-interface-rest` module exposes the public headless API that a SPA, mobile app,
or other backend can use.

Typical resources (names may evolve as the project grows):

- `Thread` – create / list / update / close threads  
- `Post` – add replies, edit posts  
- `Member` – register/link members to external identities  
- `Tenant` – configure tenant-level settings & vertical metadata  

> OpenAPI/Swagger integration is planned so you can browse the APIs in a browser.  
> For now, see controller classes in `forum-interface-rest` and tests for examples.

---

## Persistence & Migrations

The `forum-infra-jpa` module defines:

- JPA entities and repositories  
- Mapping from domain aggregates to Postgres tables  
- JSONB columns for `metadata` and config fields  
- Flyway-based schema migrations  

Testing support:

- **Testcontainers (Postgres)** for integration tests  
- **H2** for lightweight tests where appropriate  

---

## Development Notes

### Coding guidelines (short version)

From `ARCHITECTURE.md`:

- **No Spring in `forum-domain-core`**  
- Use **factories** for complex creation/validation logic  
- Aggregates have **hidden constructors**, created via factories  
- Domain events are **state-carrying** and persisted via Outbox  
- Prefer:
  - Immutability (`final`, records, unmodifiable collections)
  - Functional patterns (`map`, `flatMap`, `Optional`) where it helps readability

### Running tests

```bash
./mvnw test
```

For modules with Testcontainers, tests will start a temporary Postgres instance automatically.

---

## Roadmap (High-level)

These are intentionally **future phases**, not all implemented yet:

- **Admin Interface (`forum-interface-admin`)**  
  - Tenant management, bulk moderation tools, config management.  
- **AI Integration (`forum-interface-ai`)**  
  - AI “member” that can answer questions and summarize threads using Spring AI.  
  - AI-powered triage (toxic/urgent threads) and auto-tagging.  
- **Search & Analytics**  
  - Full-text search (Postgres FTS initially, Elasticsearch later).  
  - Analytics aggregated from Outbox events.  
- **Vertical Templates**  
  - Predefined metadata + workflows for verticals:
    - Pet lost & found, TCG retail, apartment syndication, etc.  
- **SEO & Growth Layer**  
  - SEO-friendly routes + structured data (`DiscussionForumPosting`).  
  - Optional “SEO as a service” / ads integration (Google Ads, Meta, etc.).  

---

## Contributing

This repo is currently focused on stabilizing the **core engine**.

Planned contributions:

- Additional domain aggregates & value objects  
- New vertical templates  
- Integrations (CRM, helpdesk, marketing tools)  
- Documentation & examples  

If you have ideas or find issues, feel free to open an Issue or PR.

---

## License & Usage

OpenForum is licensed under the [Apache 2.0 License](./LICENSE).

This means you are free to:

- Use it in open-source or closed-source projects
- Embed it in commercial products
- Run it as part of a hosted/SaaS offering

We welcome contributions, but you are not *obligated* to open-source your own extensions.

