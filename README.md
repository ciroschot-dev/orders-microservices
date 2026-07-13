# 📦 OrderFlow — Event-Driven Microservices Order Platform

> Order-management platform for local businesses, built as an **event-driven microservices** system with
> Java 21, Spring Boot and Spring Cloud, plus a **React demo frontend**. Two goals in one: a hands-on
> learning project on real microservices patterns, and the foundation for a sellable SaaS.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-6DB33F?logo=spring&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?logo=swagger&logoColor=black)

---

## 📑 Table of contents

- [What this project demonstrates](#-what-this-project-demonstrates)
- [Architecture](#-architecture)
- [Order lifecycle — the saga](#-order-lifecycle--the-saga)
- [Communication styles](#-communication-styles)
- [Event contracts](#-event-contracts)
- [Services](#-services)
- [Frontend demo](#-frontend-demo)
- [Tech stack](#-tech-stack)
- [Run it locally](#-run-it-locally)
- [Roadmap / status](#-roadmap--status)
- [Documentation](#-documentation)

---

## ✅ What this project demonstrates

- 🧩 Microservices with **one database per service** (no shared DB)
- 🧭 **Service discovery** (Eureka) behind a single **API Gateway**
- 🔗 **Resilient synchronous** calls — OpenFeign + Resilience4j **circuit breaker**
- 📨 **Event-driven asynchronous** messaging — RabbitMQ topic exchange, **fan-out** to independent consumers, **dead-letter queue**
- 🔄 **Saga / choreography** — an order auto-confirms (or auto-cancels) via reverse events, with no orchestrator
- 🏎️ **Concurrency correctness** — atomic conditional `UPDATE` handles the stock race (TOCTOU): the last units go to exactly one order, stock never goes negative
- 🧱 Clean **layered architecture** — DTOs at the edges, Bean Validation, RFC-7807 `ProblemDetail` errors, OpenAPI docs
- 🖥️ **React demo frontend** consuming the gateway, with the event flow visible live (stock dropping, order confirming, notifications arriving)
- 🐳 **One-command Docker Compose** — the whole backend (9 containers) comes up healthy
- ☁️ **Cloud-ready** design mapped to AWS managed services (see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md))

---

## 🏗️ Architecture

```
   ┌──────────────┐        ┌────────────────────┐
   │ React SPA    │ ─────► │   API Gateway      │   Spring Cloud Gateway (single entry point)
   │ (Vite :5173) │  /api  └─────────┬──────────┘
   └──────────────┘                  │  routes resolved by service name via Eureka
             ┌─────────────────────┼─────────────────────┐
             ▼                     ▼                       ▼
     ┌───────────────┐    ┌────────────────┐     ┌──────────────────────┐
     │ order-service │    │ inventory-svc  │     │ notification-service │
     │  PostgreSQL   │    │  PostgreSQL    │     │      MongoDB         │
     └───────┬───────┘    └───────┬────────┘     └──────────┬───────────┘
             │ 1) sync stock check │                         │
             │   (OpenFeign + CB)  │                         │
             │ 2) publish          │ 3) decrement stock,     │ notify customer
             │    order.created    │    publish result       │ (created + status)
             ▼                     ▼                         ▲
   ┌────────────────────────────────────────────────────────┴───────┐
   │                    RabbitMQ  ·  order.exchange (topic)          │
   │  order.created    ─► inventory queue  + notification queue      │  (fan-out)
   │  order.confirmed  ─► order queue      + notification queue      │  (saga return)
   │  order.cancelled  ─► order queue      + notification queue      │  (compensation)
   │                          failures → retries → dead-letter queue │
   └────────────────────────────────────────────────────────────────┘

   discovery-server (Eureka)  ◄── every service registers & discovers here
```

---

## 🔄 Order lifecycle — the saga

An order goes through an **asynchronous choreography** (no central orchestrator): each service reacts to
events and emits its own. This is the load-bearing part of the design.

1. **Create** — `POST /api/orders`. `order-service` does a **synchronous** stock pre-check against
   `inventory-service` (OpenFeign + circuit breaker), persists the order as **`PENDING`**, and — only
   after the DB commit — publishes **`order.created`**.
2. **Reserve stock** — `inventory-service` consumes `order.created` and decrements stock with a single
   **atomic conditional `UPDATE`** (`... WHERE available >= qty`). Then it publishes the result back:
   **`order.confirmed`** if every line was reserved, or **`order.cancelled`** if a race lost the last units.
3. **Settle** — `order-service` consumes the reverse event and moves the order to **`CONFIRMED`** or
   **`CANCELLED`** — with no manual step.
4. **Notify** — `notification-service` consumes `order.created` **and** the confirm/cancel events (fan-out),
   storing a "received" notification and then a "confirmed"/"cancelled" one.

> **Why the pre-check *and* the reverse event?** The sync pre-check gives fast feedback for the common case,
> but two concurrent orders for the last units can both pass it (a TOCTOU race). The atomic `UPDATE` in
> inventory is the real gatekeeper, and `order.cancelled` is the **compensating action** that keeps the
> order state consistent with the stock — eventual consistency in practice.

---

## 🔀 Communication styles

Two complementary styles, chosen on purpose:

| Style | When | How |
|---|---|---|
| 🔗 **Synchronous** (request/response) | `order-service` checks stock in `inventory-service` **before** accepting an order | **OpenFeign** declarative client + **Resilience4j circuit breaker** — a slow/down inventory never takes orders down |
| 📨 **Asynchronous** (event-driven) | Reserving stock, settling the order status, notifying the customer | services publish/consume events over **RabbitMQ** and react **independently** — the order response never waits for them |

---

## 📨 Event contracts

All events flow through the topic exchange **`order.exchange`**. Bodies are **flat DTOs** (JSON) on purpose:
consumers never depend on any service's domain model. Consumers deserialize by the listener's parameter type
(`INFERRED` type mapping), so the producer's package names don't leak across services.

**`order.created`** — published by `order-service`, consumed by **inventory + notification** (fan-out):

```json
{ "orderId": 12, "items": [{ "productId": 3, "quantity": 2 }] }
```

**`order.confirmed`** / **`order.cancelled`** — published by `inventory-service` after reserving stock,
consumed by **order-service** (set status) **+ notification-service** (notify):

```json
{ "orderId": 12 }
```

- **Delivery:** at-least-once → consumers retry with backoff; after N attempts a message is **dead-lettered**
  (`order.dlx`) instead of looping forever. Business failures (no stock) are handled in-band, never thrown.

---

## 🧩 Services

| Service | Role | Store | Port |
|---|---|---|---|
| `discovery-server` | Service registry (Eureka) | — | 8761 |
| `api-gateway` | Single entry point / routing | — | 8080 |
| `order-service` | Orders lifecycle; publishes `order.created`, consumes confirm/cancel | PostgreSQL | 8081 |
| `inventory-service` | Stock; sync check + consumes `order.created`, publishes confirm/cancel | PostgreSQL | 8082 |
| `notification-service` | Notifications; consumes created + confirm/cancel | MongoDB | 8083 |
| `frontend` | React demo SPA (dev server) | — | 5173 |

Infra (Docker): PostgreSQL `5432` (orders) / `5433` (inventory) · RabbitMQ `5672` (AMQP) / `15672` (UI) · MongoDB `27017`.

---

## 🖥️ Frontend demo

A single-page app (**Vite + React 19 + TypeScript + Tailwind**) in [`frontend/`](frontend/) that consumes the
API Gateway. Four screens: **Inventory** (product CRUD), **Create order** (with a live view of stock dropping
and the order auto-confirming), **Orders** (status + history), and **Notifications** (live, via the fan-out).
In dev, Vite proxies `/api` to the gateway, so no CORS config is needed on the backend.

It's the payoff of the event-driven design made visible: create an order and watch — within ~1s — the stock
drop, the order flip `PENDING → CONFIRMED`, and two notifications appear, all without a page reload.

---

## 🛠️ Tech stack

**Backend**

- Java 21 · Spring Boot 3.5 (Web, Data JPA, Data MongoDB, Validation, Actuator)
- Spring Cloud 2025.0 — Eureka, Spring Cloud Gateway, OpenFeign, Resilience4j
- RabbitMQ (Spring AMQP) — topic exchange, fan-out, dead-letter queues
- PostgreSQL (order, inventory) · MongoDB (notification) · MapStruct
- springdoc-openapi (Swagger UI per service)

**Frontend**

- Vite · React 19 · TypeScript · Tailwind CSS v4

**DevOps**

- Docker + Docker Compose (multi-stage builds, healthchecks) · Maven (`./mvnw`)
- Cloud target (local-first): AWS — ECS Fargate · RDS · Amazon MQ · ECR

---

## ▶️ Run it locally

Requirements: **Docker + Docker Compose** (backend) and **Node 20+** (frontend).

```bash
git clone https://github.com/ciroschot-dev/orders-microservices.git
cd orders-microservices

# 1) Backend — builds and starts all 9 containers, healthy, in one command
docker compose up -d --build      # wait until every container is healthy

# 2) Frontend — the demo SPA
cd frontend
npm install                        # first time only
npm run dev                        # → http://localhost:5173
```

> Re-running after changing queue definitions? Do `docker compose down -v` first — RabbitMQ won't redeclare a
> queue with different arguments, and `-v` also resets the databases for a clean demo.

**Explore**

| Resource | URL |
|---|---|
| 🖥️ Demo frontend | http://localhost:5173 |
| 🧭 Eureka dashboard | http://localhost:8761 |
| 🐰 RabbitMQ management | http://localhost:15672 *(guest / guest)* |
| 📘 Swagger UI (order) | http://localhost:8081/swagger-ui.html |
| 🚪 Via the gateway | `POST http://localhost:8080/api/orders` · `GET http://localhost:8080/api/products` · `GET http://localhost:8080/api/notifications` |

---

## 🗺️ Roadmap / status

- [x] **Phase 1** — `order-service`: orders CRUD, MapStruct, Testcontainers
- [x] **Phase 2** — Eureka service discovery
- [x] **Phase 3** — `inventory-service` + OpenFeign + Resilience4j circuit breaker
- [x] **Phase 4** — API Gateway (Spring Cloud Gateway)
- [x] **Phase 5** — RabbitMQ event-driven (`order.created`, fan-out, DLQ) + **saga** (confirm/cancel reverse events)
- [x] **Phase 6** — `notification-service` + MongoDB (created + status notifications)
- [x] **Phase 7** — full dockerization (`docker compose up --build` brings up everything)
- [x] **Phase 7.5** — React demo frontend
- [ ] **Phase 8** — production-grade + AWS cloud deploy

Full roadmap, decisions and current status: [`PLAN.md`](PLAN.md).

---

## 📚 Documentation

- 📄 [`PLAN.md`](PLAN.md) — vision, phased roadmap and current status. **Start here.**
- 🏗️ [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — services, ports, communication styles, event contracts,
  technical decisions (ADR-lite) and the AWS cloud mapping.
- ⚙️ [`CLAUDE.md`](CLAUDE.md) — conventions, stack and working mode.
