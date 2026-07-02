# PLAN — OrderFlow (order platform, microservices)

> Master work and learning plan. Meant for **resuming the conversation after a `/clear`**.
> If you come back and don't remember where we were, read the **CURRENT STATUS** section first.
> Working mode: **`/profesor`** skill active (see `CLAUDE.md`). Ciro writes all the code.

---

## 1. Business vision (why this project is real)

**Product:** SaaS for order management for local businesses — a business loads its products/stock,
receives orders, inventory is decremented automatically and the customer is notified. Working name: **OrderFlow**.

**Initial market (assumption, changeable):** local businesses in Mar del Plata — food spots,
health-food stores, kiosks, small shops. Connects with Ciro's current business (landings for local
businesses): first you build them the landing, then you sell them the back office that manages orders.

> Decision made: we start with the generic domain **orders + inventory + notifications**,
> which is sellable to almost any business and, at the same time, the textbook case to teach microservices
> and messaging. Alternative discarded for now: niching hard into food service (tables, kitchen, KDS) —
> can be added as a vertical later without redoing the base.

**Path to a sellable product** (Phase 8, post-learning): multi-tenancy (several businesses on one
instance), authentication, admin panel, billing/subscription, cloud deploy.

---

## 2. Why this project (career goal)

It covers in one shot the gaps detected for the Java role at Accenture and similar:

| Job posting requirement | How OrderFlow covers it |
|---|---|
| Microservices | 5 independent services |
| Spring Cloud | Eureka (discovery) + Gateway + OpenFeign + Resilience4j |
| RabbitMQ / queues | Asynchronous `OrderCreated` events (producer/consumers) |
| SQL **and** NoSQL | PostgreSQL (order/inventory) + MongoDB (notification) |
| Swagger | springdoc in every service |
| Hibernate/JPA, Maven, Git, Docker | Across the whole project |
| Java 8-21 | Java 21 |
| Cloud (AWS/Azure/Openshift) | **AWS** in Phase 8 (ECS Fargate, RDS, Amazon MQ, DocumentDB) |

---

## 3. Architecture (summary)

Full detail in `docs/ARCHITECTURE.md`. Bird's-eye view:

```
                          ┌─────────────────┐
        client   ───►     │   API Gateway   │   (Spring Cloud Gateway)
                          └────────┬────────┘
                                   │   discovers routes via Eureka
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                     ▼
       ┌────────────┐      ┌──────────────┐      ┌──────────────┐
       │   order    │      │  inventory   │      │ notification │
       │ (Postgres) │      │  (Postgres)  │      │  (MongoDB)   │
       └─────┬──────┘      └──────▲───────┘      └──────▲───────┘
             │  publishes OrderCreated    consume the event
             └──────────►  RabbitMQ  ◄───────────────────┘
                          (exchange/queues)
                   ┌────────────────────────┐
                   │  discovery-server       │  Eureka: service registry
                   │  (everyone registers)   │
                   └────────────────────────┘
```

**Key design idea:** creating an order does NOT wait for stock decrement nor for the notification.
`order` saves the order, publishes the `OrderCreated` event to RabbitMQ and responds right away.
`inventory` and `notification` react when they can (asynchronous, decoupled, resilient).

---

## 4. Phased roadmap

Each phase is **committable and adds to the CV** even if you stop there. Mark progress with `[x]`.

### Phase 0 — Setup and foundations  ·  ✅ DONE
**Goal:** repo structure + first service generated.
- [x] Root folder `orders-microservices/` created
- [x] Base documentation (`CLAUDE.md`, `PLAN.md`, `docs/ARCHITECTURE.md`, `README.md`)
- [x] `order-service` generated via IntelliJ's **New Project → Spring Boot** (built-in Spring Initializr) with:
      Maven · Java 21 · Spring Boot 3.5.15 · Group `com.ciro` · Artifact `order-service` ·
      deps: **Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, springdoc-openapi**
- [x] Created inside `orders-microservices/order-service/`
- **Learn:** what a microservice is vs a monolith · Spring Initializr · a service's Maven structure.

### Phase 1 — `order-service` production-grade  ·  ✅ DONE
**Goal:** one impeccable end-to-end service (still no microservices, focus on quality).
- [x] Postgres running in Docker (container) + `application.yaml` pointing to it (env vars, port 8081)
- [x] `Order` entity (+ `OrderItem`), domain modeling — bidirectional aggregate, `addItem`/`removeItem` helpers
- [x] Layers: controller / service / repository / dto / mapper / exception (+ `enums`)
- [x] DTOs (Java records) + Bean Validation on inputs (`@Valid` cascades into item list)
- [x] Orders CRUD: create (POST 201), list (GET), get by id (GET), change status (PATCH `/{id}/status`)
- [x] Global error handling with `@RestControllerAdvice` + `ProblemDetail` (RFC 7807) → 404 + 400 (field map)
- [x] Swagger/OpenAPI (springdoc) live at `/swagger-ui.html` (works out of the box; no custom annotations yet)
- [x] Actuator (`/actuator/health`, `show-details: always` → db health visible)
- [x] Tests: service unit (Mockito, 3) + integration with **Testcontainers** (real ephemeral Postgres, 2)
- **Learn:** pro layered architecture · DTO vs entity · ProblemDetail · Testcontainers · Actuator.

> **Decisions in Phase 1:** mapping via **MapStruct** (`componentModel = "spring"`, `@AfterMapping` to sync the
> bidirectional back-reference; Lombok+MapStruct processor order wired in `pom.xml`). Schema via
> **`ddl-auto: update`** for now — **Flyway migrations deferred** (proper prod approach, add before shipping).
> **Deferred:** web-layer tests (MockMvc for controller/validation/ProblemDetail) + deeper OpenAPI annotations.

### Phase 2 — Discovery with Eureka  ·  ✅ COMPLETE
**Goal:** services find each other on their own, no hardcoded IPs.
- [x] `discovery-server` generated (dep: Eureka Server)
- [x] `order-service` registers as a Eureka Client
- [x] Verify on the Eureka dashboard (`http://localhost:8761`)
- **Learn:** service discovery · client-side discovery · why hosts aren't hardcoded.

### Phase 3 — `inventory-service` + communication  ·  ✅ COMPLETE
**Goal:** second service and first communication between services (synchronous).
- [x] `inventory-service` (own Postgres) with `Product` entity + CRUD
- [x] `order-service` queries stock via **OpenFeign** (resolved by Eureka)
- [x] **Resilience4j** (circuit breaker / fallback) in case `inventory` is down
- **Learn:** OpenFeign · client-side load balancing · circuit breaker · resilience.

### Phase 4 — API Gateway  ·  ⚪ pending
**Goal:** a single entry door for the whole system.
- [ ] `api-gateway` (Spring Cloud Gateway) routing to order/inventory via Eureka
- [ ] Path routing (`/api/orders/**` → order, `/api/inventory/**` → inventory)
- **Learn:** API gateway pattern · routing · why a single entry point (auth, CORS, rate limit later).

### Phase 5 — RabbitMQ (event-driven)  ·  ⚪ pending  ·  ⭐ core of the project
**Goal:** decouple with asynchronous messaging.
- [ ] RabbitMQ running in Docker (with management UI `http://localhost:15672`)
- [ ] `order` publishes `OrderCreated` event (exchange + routing key) when creating an order
- [ ] `inventory` consumes `OrderCreated` and decrements stock
- [ ] Failure handling: retries / dead-letter queue (DLQ)
- **Learn:** AMQP · exchange/queue/binding · producer/consumer · idempotency · DLQ · why async.

### Phase 6 — `notification-service` + MongoDB  ·  ⚪ pending
**Goal:** add NoSQL and a second consumer of the event.
- [ ] `notification-service` with MongoDB (Docker)
- [ ] Consumes `OrderCreated` and stores/"sends" a notification (log/simulated email)
- **Learn:** when NoSQL vs SQL · Spring Data MongoDB · fan-out (several consumers, one event).

### Phase 7 — Full dockerization  ·  ⚪ pending
**Goal:** `docker compose up` brings EVERYTHING up.
- [ ] `Dockerfile` (multi-stage) per service + `.dockerignore`
- [ ] `docker-compose.yml`: postgres, mongo, rabbitmq, eureka, gateway, order, inventory, notification
- [ ] Healthchecks and startup order (`depends_on`)
- **Learn:** multi-stage builds · compose · networking between containers · healthchecks.

### Phase 7.5 — Demo frontend, built with agentic AI  ·  ⚪ future (post-learning)
**Goal:** turn the backend into a **fully functional app anyone can try in the browser** — the piece that
makes the portfolio land. Not the sellable SaaS yet (no multi-tenancy/billing); a polished, public demo.
**CV angle (the point):** build it *primarily with agentic AI tooling* (Claude Code, v0, etc.) so it becomes
a first-class CV line — "agentic programming / AI-assisted development" — which is highly valued today.
- [ ] Frontend SPA (React/Next or similar) consuming the **API Gateway** (Phase 4), not the services directly
- [ ] Core screens: manage products/stock · create an order (with the live stock check) · view order
      status & history · see notifications (Phase 6 fan-out)
- [ ] Built with an **agentic workflow** on purpose — document how it was done (prompts, iterations) as part
      of the portfolio story
- [ ] **Public live demo**: deploy frontend + backend somewhere anyone can click through it with seed data
      and a guided happy path (no local setup required)
- **Learn / CV:** agentic development end-to-end · wiring a real UI to a microservices backend via the
  gateway · product thinking (turning APIs into something a human actually uses).
> Placement note: numbered 7.5 to sit between "system works" (7) and "sellable SaaS" (8) without renumbering.
> Can be promoted/reordered later.

### Phase 8 — Path to a sellable SaaS  ·  ⚪ future (post-learning)
**Cloud strategy: local-first.** Everything is developed and tested locally with Docker (free). AWS only
comes in once the whole system runs (post Phase 7). Don't deploy on each phase: avoid cost and complexity.
For CV/interview it's enough to have deployed it once, have the diagram and know how to explain it (it can
be turned off to avoid paying). AWS mapping in `docs/ARCHITECTURE.md` → "Cloud target (AWS)".
- [ ] Deploy on **AWS**: services on **ECS Fargate** · **RDS** (Postgres) · **DocumentDB** or Mongo Atlas ·
      **Amazon MQ** (RabbitMQ) · **ECR** (images) · **Secrets Manager** · CI/CD with **GitHub Actions**
- [ ] (Kubernetes → see optional Phase 9 below)
- [ ] Authentication/authorization (Spring Security + JWT or OAuth2)
- [ ] Multi-tenancy (several isolated businesses)
- [ ] Admin panel (frontend) + billing/subscription
- [ ] **Electronic invoicing with ARCA (ex-AFIP)** — feature that makes the product truly sellable
- [ ] Observability (centralized logs, metrics, distributed tracing)

### Phase 9 (OPTIONAL) — Kubernetes  ·  ⚪ future · do only after Phase 8 works
**Why:** Accenture and most serious Java/cloud roles list **Openshift**, which *is* Kubernetes.
Running the system on k8s turns the CV line from "builds APIs" into "runs microservices in production".
Not required for the project to work (Fargate already deploys it) — this is a dedicated learning layer.

**Rule:** don't start here. Build local (0–7) → deploy to Fargate (8) → only then redeploy on k8s.
Practice **free** with `minikube`/`kind` locally before paying for managed clusters.

- [ ] Run the system on a **local cluster** (`minikube` or `kind`) — zero cost, full k8s experience
- [ ] Write k8s manifests: `Deployment` + `Service` per microservice, `ConfigMap`/`Secret` for config
- [ ] Use k8s built-ins and notice the overlap with Spring Cloud: self-healing (restarts), scaling
      (replicas), rolling updates (zero-downtime deploy), service discovery (cluster DNS)
- [ ] (Cloud) deploy the same manifests to **AWS EKS** — the managed-Kubernetes equivalent of Openshift
- **Learn:** declarative orchestration · pods/deployments/services · ConfigMap/Secret · self-healing ·
  auto-scaling · rolling updates · why these patterns exist (you built them by hand in earlier phases).

---

## 5. CURRENT STATUS  📍

> **Update this section at the end of every session.** It's the first thing read on resume.

- **Phase:** 3 ✅ **COMPLETE**. Next up: **Phase 4 (API Gateway)**.
- **Done — Phase 0:** environment (Java 21, Maven, Docker/OrbStack). Monorepo `orders-microservices/`,
  remote `origin` = `git@github.com:ciroschot-dev/orders-microservices.git`. `order-service` generated
  (Maven · Java 21 · Boot 3.5.15 · group `com.ciro`).
- **Done — Phase 1 (`order-service`, branch `feat/phase-1-order-service`, all committed):**
  a full production-grade REST service, tested end-to-end.
  - **Infra:** `docker-compose.yml` at the repo root (single `postgres:16-alpine`, db `orders`, port 5432,
    named volume). `application.yaml` wired via env vars (`${DB_URL:...}`), `server.port: 8081`.
  - **Domain:** `Order` + `OrderItem` (bidirectional `@OneToMany`/`@ManyToOne`, owning side = `OrderItem`,
    `addItem`/`removeItem` helpers, `cascade=ALL`+`orphanRemoval`, LAZY, `@CreationTimestamp`, status enum
    stored as STRING). `productId` is a **logical reference** to the future inventory-service (no cross-DB FK).
  - **Layers:** `controller` (REST, `@Valid`, 201/200, PATCH for status) · `service` (`@Transactional`,
    dirty-checking update, `@RequiredArgsConstructor`) · `repository` (`JpaRepository`, `findByStatus`) ·
    `dto` (records + Bean Validation) · `mapper` (**MapStruct**, `@AfterMapping` back-ref sync) ·
    `exception` (`OrderNotFoundException` + `@RestControllerAdvice` → `ProblemDetail`).
  - **Ops:** Swagger UI (`/swagger-ui.html`) + Actuator (`/actuator/health` with db details).
  - **Tests:** `OrderServiceTest` (Mockito, 3) + `OrderServiceIntegrationTest` (Testcontainers, 2). All green.
  - **Pending push:** commits are local on `feat/phase-1-order-service`; Ciro pushes + opens the PR.
- **Key decisions (Phase 1):** DTOs = Java **records** · mapping = **MapStruct** (Lombok+MapStruct processor
  order set in `pom.xml`) · schema = **`ddl-auto: update`**, **Flyway deferred** to before shipping ·
  service = concrete class (no interface, single impl). **Deferred:** web-layer MockMvc tests + richer
  OpenAPI annotations. Working mode: `/profesor` (Ciro writes all code; Claude may add English comments on request).
- **Done — Phase 2 (Eureka, branch `feat/phase-2-eureka`):**
  - `discovery-server` created (Boot 3.5.16, Spring Cloud 2025.0.3, dep: Eureka Server only). `@EnableEurekaServer`
    on the main class; `application.yaml` → `server.port: 8761`, standalone (`register-with-eureka: false`,
    `fetch-registry: false`).
  - `order-service` turned into a Eureka client: added `spring-cloud-starter-netflix-eureka-client` + the
    `spring-cloud-dependencies` BOM (`spring-cloud.version: 2025.0.3`) to its `pom`; `application.yaml` →
    `eureka.client.service-url.defaultZone: http://localhost:8761/eureka/`. Its `spring.application.name`
    (`order-service`) is the registry ID.
  - Verified: `ORDER-SERVICE` shows **UP** on the dashboard (registration `204`). The red "self-preservation"
    banner is expected in local dev (1 client → renews < threshold); left ON on purpose.
- **Done — Phase 3 (inventory + OpenFeign + Resilience4j, branch `feat/phase-3-inventory`, all committed):**
  - **`inventory-service`** (port 8082, Boot 3.5.16, Eureka client, own Postgres): full CRUD.
    - **Infra:** a *second* Postgres container in the root `docker-compose.yml` — `inventory-postgres`
      (host port **5433**, db `inventory`, own volume). The orders one was renamed `order-postgres`.
      Rationale: **database-per-service** (no shared DB).
    - **Layers (same conventions as order):** `Product` entity (id, name, unique `sku`, `availableQuantity`)
      + `ProductRepository` (`findBySku`) · `ProductRequest`/`ProductResponse` records + Bean Validation ·
      **MapStruct** `ProductMapper` (wired into the pom) · `ProductService` (create validates SKU dup;
      update via dirty checking; delete guards with `existsById`) · `ProductController` (`/api/products`,
      POST 201, GET, PUT/{id}, DELETE 204, ids via path) · `exception` (`ProductNotFoundException` 404,
      `DuplicateSkuException` **409**, validation 400) + `@RestControllerAdvice` → `ProblemDetail`.
  - **`order-service` → `inventory-service` via OpenFeign:** `spring-cloud-starter-openfeign`,
    `@EnableFeignClients`, `InventoryClient` targets `inventory-service` **by Eureka name** (no host).
    `createOrder` now checks stock for each item *before* persisting (guard). Only **checks**, never
    decrements — the decrement is deferred to the async RabbitMQ flow (Phase 5).
    - Insufficient stock → `InsufficientStockException` **409**. Unknown product (Feign 404) → translated
      to `ProductNotFoundException` **404** (Ciro's call: referenced entity not found).
  - **Resilience4j circuit breaker:** `InventoryGateway` (its own bean, to dodge AOP self-invocation) wraps
    the Feign call with `@CircuitBreaker(name="inventory", fallbackMethod=...)`. **Key subtlety learned:**
    `ignore-exceptions` only stops an exception from *tripping* the breaker — it does **not** skip the
    fallback. So the fallback itself inspects the `Throwable`: re-throws `ProductNotFoundException` (→ 404),
    everything else → `InventoryUnavailableException` **503**. `ignore-exceptions: ProductNotFoundException`
    so 404s don't open the breaker. Instance config: window 10, min-calls 5, 50% threshold, open 10s.
  - **Verified end-to-end (Swagger):** 201 normal · 409 insufficient stock · 404 unknown product ·
    503 when inventory is down, breaker opens after threshold and short-circuits, recovers via half-open.
- **Key decisions (Phase 3):** single `Product` entity (no separate `Stock` table — YAGNI) · 409 for
  duplicate/insufficient · 404 for unknown referenced product · circuit breaker in a dedicated bean ·
  fallback does the business-vs-outage distinction. **Interview Q&A** kept in `docs/INTERVIEW-PREP.md`.
- **Resume here (Phase 4 — API Gateway):** generate `api-gateway` (Spring Cloud Gateway), route
  `/api/orders/**` → order and `/api/inventory/**` (or `/api/products/**`) → inventory, resolved via Eureka.
  New branch `feat/phase-4-gateway` off `main` after the Phase 3 PR merges. See Phase 4 checklist above.
- **To decide later:** product's final name · whether to niche into food service.

---

## 6. How to resume after `/clear`

1. Read this **CURRENT STATUS** section (above).
2. Reactivate professor mode: `/profesor` (or tell me). See rules in `CLAUDE.md`.
3. Continue from "Immediate next step".
