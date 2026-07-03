# 📦 OrderFlow — Event-Driven Microservices Order Platform

> Order-management platform for local businesses, built as an **event-driven microservices** system with
> Java 21, Spring Boot and Spring Cloud. Two goals in one: a hands-on learning project on real
> microservices patterns, and the foundation for a sellable SaaS.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-6DB33F?logo=spring&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-6-47A248?logo=mongodb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?logo=swagger&logoColor=black)
![Testcontainers](https://img.shields.io/badge/Tests-Testcontainers-291A38?logo=testcontainers&logoColor=white)

---

## 📑 Table of contents

- [What this project demonstrates](#-what-this-project-demonstrates)
- [Architecture](#-architecture)
- [Communication styles](#-communication-styles)
- [Event contract — `OrderCreated`](#-event-contract--ordercreated)
- [Services](#-services)
- [Tech stack](#-tech-stack)
- [Run it locally](#-run-it-locally)
- [Roadmap / status](#-roadmap--status)
- [Documentation](#-documentation)

---

## ✅ What this project demonstrates

- 🧩 Microservices with **one database per service** (no shared DB)
- 🧭 **Service discovery** (Eureka) behind a single **API Gateway**
- 🔗 **Resilient synchronous** calls — OpenFeign + Resilience4j **circuit breaker**
- 📨 **Event-driven asynchronous** messaging — RabbitMQ topic exchange, idempotent consumers, **dead-letter queue**
- 🧱 Clean **layered architecture** — DTOs at the edges, Bean Validation, RFC-7807 `ProblemDetail` errors, OpenAPI docs
- 🧪 **Integration testing with Testcontainers** (real ephemeral databases)
- ☁️ **Cloud-ready** design mapped to AWS managed services (see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md))

---

## 🏗️ Architecture

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
   │  RabbitMQ  (order.exchange)│──► inventory queue   (decrement stock)
   │  routing key: order.created│──► notification queue (notify customer)
   └───────────────────────────┘         failures → retries → dead-letter queue

   discovery-server (Eureka)  ◄── every service registers & discovers here
```

---

## 🔀 Communication styles

Two complementary styles, chosen on purpose:

| Style | When | How |
|---|---|---|
| 🔗 **Synchronous** (request/response) | `order-service` checks stock in `inventory-service` **before** accepting an order | **OpenFeign** declarative client + **Resilience4j circuit breaker** — a slow/down inventory never takes orders down |
| 📨 **Asynchronous** (event-driven) | Once an order is accepted, other services react to it | `order-service` publishes `OrderCreated` to **RabbitMQ**; inventory (decrement stock) and notification (notify customer) consume **independently** — the order response never waits for them |

The async path is the load-bearing decision: **low latency, decoupling and resilience**.

---

## 📨 Event contract — `OrderCreated`

Published by `order-service` after an order is persisted. Consumers are **idempotent** (at-least-once delivery).

```json
{
  "eventId": "uuid",
  "occurredAt": "ISO-8601 timestamp",
  "orderId": "uuid",
  "customerId": "uuid",
  "items": [{ "productId": "uuid", "quantity": 3 }]
}
```

- **Exchange:** `order.exchange` (topic) · **Routing key:** `order.created`
- **Failure handling:** retries with backoff → after N attempts → **dead-letter queue** for inspection

---

## 🧩 Services

| Service | Role | Store | Port |
|---|---|---|---|
| `discovery-server` | Service registry (Eureka) | — | 8761 |
| `api-gateway` | Single entry point / routing | — | 8080 |
| `order-service` | Orders lifecycle, publishes `OrderCreated` | PostgreSQL | 8081 |
| `inventory-service` | Stock, sync check + consumes event | PostgreSQL | 8082 |
| `notification-service` | Notifications, consumes event | MongoDB | 8083 |

Infra (Docker): PostgreSQL `5432` (orders) / `5433` (inventory) · RabbitMQ `5672` (AMQP) / `15672` (UI) · MongoDB `27017`.

---

## 🛠️ Tech stack

**Core**

- Java 21
- Spring Boot 3.5 (Web, Data JPA, Validation, Actuator)
- Spring Cloud 2025.0 — Eureka, Spring Cloud Gateway, OpenFeign, Resilience4j
- MapStruct (DTO ↔ entity mapping)

**Messaging & data**

- RabbitMQ (Spring AMQP) — topic exchange, dead-letter queues
- PostgreSQL (order, inventory) · MongoDB (notification)

**Docs, testing & DevOps**

- springdoc-openapi (Swagger UI per service)
- Testcontainers (integration tests with real ephemeral databases)
- Docker + Docker Compose · Maven (`./mvnw`)

**Cloud target (local-first)**

- AWS — ECS Fargate · RDS · Amazon MQ · ECR — optional EKS / Kubernetes

---

## ▶️ Run it locally

Requirements: **Docker + Docker Compose** and **Java 21**. Services currently run from your IDE/Maven;
full one-command dockerization is Phase 7.

```bash
git clone https://github.com/ciroschot-dev/orders-microservices.git
cd orders-microservices

# 1) start infra (PostgreSQL x2 + RabbitMQ)
docker compose up -d

# 2) start the services in order (each in its own terminal, or from the IDE)
cd discovery-server    && ./mvnw spring-boot:run   # Eureka  → http://localhost:8761
cd ../api-gateway      && ./mvnw spring-boot:run   # Gateway → http://localhost:8080
cd ../order-service    && ./mvnw spring-boot:run   # Orders  → http://localhost:8081
cd ../inventory-service && ./mvnw spring-boot:run
```

**Explore**

| Resource | URL |
|---|---|
| 🧭 Eureka dashboard | http://localhost:8761 |
| 🐰 RabbitMQ management | http://localhost:15672 *(guest / guest)* |
| 📘 Swagger UI (order) | http://localhost:8081/swagger-ui.html |
| 🚪 Everything via the gateway | `POST http://localhost:8080/api/orders` · `GET http://localhost:8080/api/products` |

---

## 🗺️ Roadmap / status

- [x] **Phase 1** — `order-service`: orders CRUD, MapStruct, Testcontainers
- [x] **Phase 2** — Eureka service discovery
- [x] **Phase 3** — `inventory-service` + OpenFeign + Resilience4j circuit breaker
- [x] **Phase 4** — API Gateway (Spring Cloud Gateway)
- [ ] **Phase 5** — RabbitMQ event-driven (`OrderCreated`, DLQ) — *in progress* ⏳
- [ ] **Phase 6** — `notification-service` + MongoDB
- [ ] **Phase 7** — full dockerization (`docker compose up` brings up everything)

Full roadmap, decisions and current status: [`PLAN.md`](PLAN.md).

---

## 📚 Documentation

- 📄 [`PLAN.md`](PLAN.md) — vision, phased roadmap and current status. **Start here.**
- 🏗️ [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — services, ports, communication styles, event contract,
  technical decisions (ADR-lite) and the AWS cloud mapping.
- ⚙️ [`CLAUDE.md`](CLAUDE.md) — conventions, stack and working mode.
