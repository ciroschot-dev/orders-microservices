# OrderFlow — Event-Driven Microservices Order Platform

Order-management platform for local businesses, built as an **event-driven microservices** system with
**Java 21, Spring Boot 3.5 and Spring Cloud**. Two goals in one: a hands-on learning project on real
microservices patterns, and the foundation for a sellable SaaS.

## Stack

**Java 21** · **Spring Boot 3.5** · **Spring Cloud 2025.0** (Eureka · Spring Cloud Gateway · OpenFeign ·
Resilience4j) · **RabbitMQ** (AMQP) · **PostgreSQL** · **MongoDB** · MapStruct · springdoc-openapi
(Swagger) · **Testcontainers** · **Docker Compose**.
Cloud target: **AWS** (ECS Fargate · RDS · Amazon MQ · ECR) — optional **EKS / Kubernetes**.

## Architecture

```
                         ┌────────────────────┐
        client  ──────►  │   API Gateway      │   Spring Cloud Gateway (single entry point)
                         └─────────┬──────────┘
                                   │  routes resolved by service name via Eureka
             ┌─────────────────────┼─────────────────────┐
             ▼                     ▼                       ▼
     ┌───────────────┐    ┌────────────────┐     ┌──────────────────────┐
     │ order-service │    │ inventory-svc  │     │ notification-service │
     │  PostgreSQL   │    │  PostgreSQL    │     │      MongoDB         │
     └───────┬───────┘    └────────────────┘     └──────────────────────┘
             │  1) sync stock check (OpenFeign + Resilience4j circuit breaker)
             │
             │  2) publish OrderCreated
             ▼
   ┌───────────────────────────┐
   │  RabbitMQ  (order.exchange)│──► inventory queue  (decrement stock)
   │  routing key: order.created│──► notification queue (notify customer)
   └───────────────────────────┘        failures → retries → dead-letter queue

   discovery-server (Eureka)  ◄── every service registers & discovers here
```

Two communication styles, chosen on purpose:

- **Synchronous** — `order-service` checks stock in `inventory-service` before accepting an order, via
  **OpenFeign** with a **Resilience4j circuit breaker** so a slow/down inventory never takes orders down.
- **Asynchronous (event-driven)** — once an order is accepted it publishes an `OrderCreated` event to
  **RabbitMQ**; `inventory-service` (decrement stock) and `notification-service` (notify customer) consume
  it independently. Consumers are **idempotent** (at-least-once delivery); failures go to a **dead-letter
  queue**. This is the load-bearing decision: low latency, decoupling, resilience.

## Services

| Service | Role | Store | Port |
|---|---|---|---|
| discovery-server | Service registry (Eureka) | — | 8761 |
| api-gateway | Single entry point / routing | — | 8080 |
| order-service | Orders lifecycle, publishes `OrderCreated` | PostgreSQL | 8081 |
| inventory-service | Stock, sync check + consumes event | PostgreSQL | 8082 |
| notification-service | Notifications, consumes event | MongoDB | 8083 |

Infra (Docker): PostgreSQL `5432` (orders) / `5433` (inventory) · RabbitMQ `5672` (AMQP) / `15672` (UI) ·
MongoDB `27017`.

## Run it locally

Requirements: **Docker + Docker Compose** and **Java 21** (services currently run from your IDE/Maven;
full one-command dockerization is Phase 7).

```bash
git clone https://github.com/ciroschot-dev/orders-microservices.git
cd orders-microservices

# 1) start infra (PostgreSQL x2 + RabbitMQ)
docker compose up -d

# 2) start the services in order (each in its own terminal, or from the IDE)
cd discovery-server && ./mvnw spring-boot:run   # Eureka   → http://localhost:8761
cd ../api-gateway    && ./mvnw spring-boot:run   # Gateway  → http://localhost:8080
cd ../order-service  && ./mvnw spring-boot:run   # Orders   → http://localhost:8081
cd ../inventory-service && ./mvnw spring-boot:run
```

Explore:

- **Eureka dashboard** — http://localhost:8761 (services registered)
- **RabbitMQ management** — http://localhost:15672 (guest / guest)
- **Swagger UI** per service — e.g. http://localhost:8081/swagger-ui.html
- **Everything through the gateway** — e.g. `POST http://localhost:8080/api/orders`,
  `GET http://localhost:8080/api/products`

## What this project demonstrates

- Microservices with **one database per service** (no shared DB)
- **Service discovery** (Eureka) behind a single **API Gateway**
- **Resilient synchronous** calls (OpenFeign + Resilience4j circuit breaker)
- **Event-driven asynchronous** messaging (RabbitMQ, topic exchange, idempotent consumers, dead-letter queue)
- Clean **layered architecture**, DTOs at the edges, Bean Validation, RFC-7807 `ProblemDetail` errors, OpenAPI docs
- **Integration testing with Testcontainers** (real ephemeral databases)
- **Cloud-ready** design mapped to AWS managed services (see `docs/ARCHITECTURE.md`)

## Roadmap / status

- [x] **Phase 1** — `order-service`: orders CRUD, MapStruct, Testcontainers
- [x] **Phase 2** — Eureka service discovery
- [x] **Phase 3** — `inventory-service` + OpenFeign + Resilience4j circuit breaker
- [x] **Phase 4** — API Gateway (Spring Cloud Gateway)
- [ ] **Phase 5** — RabbitMQ event-driven (`OrderCreated`, DLQ) — *in progress*
- [ ] **Phase 6** — `notification-service` + MongoDB
- [ ] **Phase 7** — full dockerization (`docker compose up` brings up everything)

Full roadmap, decisions and current status: [`PLAN.md`](PLAN.md).

## Documentation

- [`PLAN.md`](PLAN.md) — vision, phased roadmap and current status. Start here.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — services, ports, communication styles, event contract,
  technical decisions (ADR-lite) and the AWS cloud mapping.
- [`CLAUDE.md`](CLAUDE.md) — conventions, stack and working mode.
