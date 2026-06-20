# Architecture — OrderFlow

Technical reference for the microservices system. For the roadmap and current status see `../PLAN.md`.
For working conventions see `../CLAUDE.md`.

## Services & ports

| Service              | Responsibility                                  | Data store      | Port  |
|----------------------|-------------------------------------------------|-----------------|-------|
| `discovery-server`   | Eureka registry — services register & discover  | —               | 8761  |
| `api-gateway`        | Single entry point, routing, cross-cutting      | —               | 8080  |
| `order-service`      | Orders lifecycle, publishes `OrderCreated`      | PostgreSQL      | 8081  |
| `inventory-service`  | Product stock, consumes `OrderCreated`          | PostgreSQL      | 8082  |
| `notification-service`| Customer notifications, consumes `OrderCreated`| MongoDB         | 8083  |

Infra (Docker): PostgreSQL `5432`, MongoDB `27017`, RabbitMQ `5672` (AMQP) / `15672` (management UI).

> Ports are a convention; the gateway hides them from clients. Internal calls resolve service names
> via Eureka, never hardcoded host:port.

## Communication styles

Two complementary styles, used on purpose:

1. **Synchronous (request/response)** — `order-service` → `inventory-service` to *check* stock before
   accepting an order. Done with **OpenFeign** (declarative HTTP client) + **Resilience4j** circuit
   breaker so a slow/down inventory doesn't take order down.

2. **Asynchronous (event-driven)** — once an order is accepted, `order-service` publishes an
   `OrderCreated` event to **RabbitMQ**. `inventory-service` (decrement stock) and
   `notification-service` (notify customer) consume it independently. The order response does **not**
   wait for them. This is the load-bearing design decision: low latency, decoupling, resilience.

```
order-service ──publish──► [ RabbitMQ exchange: order.exchange ]
                                   │ routing key: order.created
                   ┌───────────────┴───────────────┐
                   ▼                               ▼
        queue: inventory.order.created   queue: notification.order.created
                   │                               │
          inventory-service                notification-service
        (decrement stock)              (store + "send" notification)
```

## Event contract — `OrderCreated`

Published by `order-service` after an order is persisted. Consumers must be **idempotent**
(the same event may be delivered more than once — at-least-once delivery).

```json
{
  "eventId": "uuid",
  "occurredAt": "ISO-8601 timestamp",
  "orderId": "uuid",
  "customerId": "uuid",
  "items": [
    { "productId": "uuid", "quantity": 3 }
  ]
}
```

- **Exchange:** `order.exchange` (topic)
- **Routing key:** `order.created`
- **Failure handling:** retries with backoff; after N attempts → **dead-letter queue** for inspection.

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
