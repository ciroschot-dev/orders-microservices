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

### Phase 0 — Setup and foundations  ·  🟡 IN PROGRESS
**Goal:** repo structure + first service generated.
- [x] Root folder `orders-microservices/` created
- [x] Base documentation (`CLAUDE.md`, `PLAN.md`, `docs/ARCHITECTURE.md`, `README.md`)
- [ ] `order-service` generated via IntelliJ's **New Project → Spring Boot** (built-in Spring Initializr) with:
      Maven · Java 21 · Spring Boot 3.5.x · Group `com.cirschot` · Artifact `order-service` ·
      deps: **Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok**
- [ ] Created inside `orders-microservices/order-service/`
- **Learn:** what a microservice is vs a monolith · Spring Initializr · a service's Maven structure.

### Phase 1 — `order-service` production-grade  ·  ⚪ pending
**Goal:** one impeccable end-to-end service (still no microservices, focus on quality).
- [ ] Postgres running in Docker (container) + `application.yml` pointing to it
- [ ] `Order` entity (+ `OrderItem`), domain modeling
- [ ] Layers: controller / service / repository / dto / mapper / exception
- [ ] DTOs + Bean Validation on inputs
- [ ] Orders CRUD (create, list, get by id, change status)
- [ ] Global error handling with `@RestControllerAdvice` + `ProblemDetail` (RFC 7807)
- [ ] Swagger/OpenAPI (springdoc) documented
- [ ] Actuator (`/actuator/health`)
- [ ] Tests: service unit + integration with **Testcontainers** (real ephemeral Postgres)
- **Learn:** pro layered architecture · DTO vs entity · ProblemDetail · Testcontainers · Actuator.

### Phase 2 — Discovery with Eureka  ·  ⚪ pending
**Goal:** services find each other on their own, no hardcoded IPs.
- [ ] `discovery-server` generated (dep: Eureka Server)
- [ ] `order-service` registers as a Eureka Client
- [ ] Verify on the Eureka dashboard (`http://localhost:8761`)
- **Learn:** service discovery · client-side discovery · why hosts aren't hardcoded.

### Phase 3 — `inventory-service` + communication  ·  ⚪ pending
**Goal:** second service and first communication between services (synchronous).
- [ ] `inventory-service` (own Postgres) with `Product`/`Stock` entity + CRUD
- [ ] `order-service` queries stock via **OpenFeign** (resolved by Eureka)
- [ ] **Resilience4j** (circuit breaker / fallback) in case `inventory` is down
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

- **Phase:** 0 (Setup) — in progress.
- **Done:** environment verified (Java 21, Maven 3.9, Docker + Compose v5). Base documentation written.
  Decisions made: new orders project · scope = full project (7 phases) · SQL DB = PostgreSQL ·
  cloud = **AWS** (local-first: deploy only in Phase 8) · future feature: invoicing with ARCA.
  Project translated to English (docs + working language).
- **Immediate next step:** Ciro generates `order-service` via IntelliJ's **New Project → Spring Boot**
  wizard with the Phase 0 config, inside `orders-microservices/order-service/`. Then: review the
  generated structure together before touching code.
- **To decide later:** product's final name · whether to niche into food service.

---

## 6. How to resume after `/clear`

1. Read this **CURRENT STATUS** section (above).
2. Reactivate professor mode: `/profesor` (or tell me). See rules in `CLAUDE.md`.
3. Continue from "Immediate next step".
