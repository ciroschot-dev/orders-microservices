# Architecture — OrderFlow

Technical reference for the microservices system. For the roadmap and current status see `../PLAN.md`.
For working conventions see `../CLAUDE.md`.

## Services & ports

| Service              | Responsibility                                  | Data store      | Port  |
|----------------------|-------------------------------------------------|-----------------|-------|
| `discovery-server`   | Eureka registry — services register & discover  | —               | 8761  |
| `api-gateway`        | Single entry point, routing, cross-cutting      | —               | 8080  |
| `order-service`      | Orders lifecycle; publishes `order.created`, consumes confirm/cancel | PostgreSQL | 8081  |
| `inventory-service`  | Product stock; consumes `order.created`, publishes confirm/cancel   | PostgreSQL | 8082  |
| `notification-service`| Customer notifications; consumes created + confirm/cancel          | MongoDB    | 8083  |
| `frontend`           | React demo SPA (Vite dev server), consumes the gateway              | —          | 5173  |

Infra (Docker): PostgreSQL `5432` (orders) / `5433` (inventory), MongoDB `27017`, RabbitMQ `5672` (AMQP) / `15672` (management UI).

> Ports are a convention; the gateway hides them from clients. Internal calls resolve service names
> via Eureka, never hardcoded host:port.

## Communication styles

Two complementary styles, used on purpose:

1. **Synchronous (request/response)** — `order-service` → `inventory-service` to *check* stock before
   accepting an order. Done with **OpenFeign** (declarative HTTP client) + **Resilience4j** circuit
   breaker so a slow/down inventory doesn't take order down.

2. **Asynchronous (event-driven)** — once an order is accepted, `order-service` publishes `order.created`
   to **RabbitMQ**. `inventory-service` (decrement stock) and `notification-service` (notify customer)
   consume it independently (**fan-out**). The order response does **not** wait for them. This is the
   load-bearing design decision: low latency, decoupling, resilience.

3. **Saga / choreography** — after reserving stock, `inventory-service` publishes a **reverse event**
   (`order.confirmed` or `order.cancelled`). `order-service` consumes it to settle the order status, and
   `notification-service` consumes it to send a follow-up notification. No central orchestrator: each
   service reacts to events and emits its own.

```
order-service ──publish──► [ RabbitMQ exchange: order.exchange (topic) ]
                                   │ order.created           (fan-out)
                   ┌───────────────┴───────────────┐
                   ▼                               ▼
        queue: order.created.inventory   queue: order.created.notification
                   │                               │
          inventory-service                notification-service
        (atomic decrement) ──┐            (store "received" notification)
                             │ publish order.confirmed / order.cancelled
                   ┌─────────┴─────────────────────┐
                   ▼                               ▼
          order-service                   notification-service
        (order → CONFIRMED / CANCELLED)   (store status notification)
```

## Event contracts

All events flow through the topic exchange `order.exchange`. Bodies are **flat DTOs** on purpose — consumers
never depend on any service's domain model — and are deserialized by the listener's parameter type
(`INFERRED` type mapping), so producer package names don't leak. Delivery is **at-least-once**, so consumers
should be idempotent.

**`order.created`** — published by `order-service` after the order is persisted (on transaction commit).
Consumed by `inventory-service` **and** `notification-service` (fan-out).

```json
{ "orderId": 12, "items": [ { "productId": 3, "quantity": 2 } ] }
```

**`order.confirmed`** / **`order.cancelled`** — published by `inventory-service` after reserving stock
(all lines reserved → confirmed; a race lost the last units → cancelled). Consumed by `order-service`
(set final status) **and** `notification-service` (follow-up notification).

```json
{ "orderId": 12 }
```

- **Exchange:** `order.exchange` (topic) · **Routing keys:** `order.created`, `order.confirmed`, `order.cancelled`
- **Failure handling:** retries with backoff; after N attempts → **dead-letter queue** (`order.dlx`) for
  inspection. Business failures (no stock) are handled in-band and never thrown, so they don't reach the DLQ.
- **Why the reverse event:** the synchronous pre-check gives fast feedback, but two concurrent orders for the
  last units can both pass it (TOCTOU). The atomic conditional `UPDATE` in inventory is the real gatekeeper,
  and `order.cancelled` is the **compensating action** — eventual consistency between order state and stock.

## Cross-cutting conventions (per service)

- **Layered architecture** — controller / service / repository / dto / mapper / model / exception / config.
- **DTOs at the edges** — never expose JPA entities over HTTP.
- **Bean Validation** on every input (`@Valid`).
- **Global error handling** — `@RestControllerAdvice` returning **`ProblemDetail`** (RFC 7807).
- **OpenAPI/Swagger** — springdoc, UI at `/swagger-ui.html` per service.
- **Actuator** — at least `/actuator/health` exposed (used by Docker healthchecks).
- **Config** — `application.yml` + Spring profiles + env vars. No secrets in source.
- **Tests** — service unit tests + integration tests with **Testcontainers** (real ephemeral DB).

## Cloud target (AWS) — local-first

Development and learning happen **locally with Docker** (zero cost). AWS is the deployment target for
the final phase, once the whole system runs locally. Each local piece maps to a managed AWS service —
**the application code does not change**, only configuration/connection strings:

| Local (Docker)                | AWS managed service                       |
|-------------------------------|-------------------------------------------|
| Service containers            | **ECS Fargate** (run containers serverless) |
| PostgreSQL                    | **RDS** for PostgreSQL                     |
| MongoDB                       | **DocumentDB** (or MongoDB Atlas)         |
| RabbitMQ                      | **Amazon MQ** (managed RabbitMQ)          |
| Docker images                 | **ECR**                                   |
| Secrets / config              | **Secrets Manager** / SSM Parameter Store |
| Deploy pipeline               | **GitHub Actions → AWS**                  |

Eureka and the API Gateway keep running as our own containers (the Spring Cloud stack is exactly what
target job posts ask for). Optional: migrate Fargate → **EKS** (Kubernetes) to cover "Openshift"-style
requirements. Cost note: deploy to demo/sell, then tear down or stop to avoid charges; the architecture
and the experience remain on the CV.

## Decisions (ADR-lite)

- **PostgreSQL for order/inventory:** relational data with clear integrity needs (orders, items, stock).
- **MongoDB for notification:** notification documents are schemaless-ish, write-heavy, no joins → NoSQL fits.
- **RabbitMQ over Kafka:** simpler ops for this scale, great Spring AMQP support, enough for the use case.
  Kafka would be the choice if we needed high-throughput event streaming / replay — not needed yet.
- **Eureka over Consul/k8s DNS:** classic Spring Cloud stack, exactly what target job posts ask for.
- **One DB per service:** each service owns its data; no shared database. Core microservices principle.
- **Saga by choreography (not orchestration):** the order settles via reverse events (`order.confirmed` /
  `order.cancelled`) with no central orchestrator. Simpler for this small flow; an orchestrator would earn
  its keep with more steps or cross-service rollbacks.
- **Atomic conditional `UPDATE` for stock:** `... SET qty = qty - :n WHERE qty >= :n` decrements only if
  stock allows, in one statement. Handles the concurrent-order race without pessimistic locks; the rows
  affected (0 or 1) is the business answer that drives confirm vs cancel.
