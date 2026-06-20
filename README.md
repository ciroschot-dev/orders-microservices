# OrderFlow — order platform (microservices)

SaaS for order management for local businesses, built as an **event-driven microservices**
system with Spring Boot + Spring Cloud + RabbitMQ. Dual goal: a sellable product and a professional
learning project on microservices architecture.

## Documentation

- **[`PLAN.md`](PLAN.md)** — business vision, phased roadmap and **current status**. Start here.
- **[`CLAUDE.md`](CLAUDE.md)** — conventions, stack and working mode (`/profesor` skill).
- **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)** — services, ports, communication and technical decisions.

## Stack

Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 (Eureka, Gateway, OpenFeign, Resilience4j) ·
PostgreSQL · MongoDB · RabbitMQ · springdoc-openapi · Testcontainers · Docker Compose.

## Services

| Service | Role | Store | Port |
|---|---|---|---|
| discovery-server | Registry (Eureka) | — | 8761 |
| api-gateway | Entry point | — | 8080 |
| order-service | Orders | PostgreSQL | 8081 |
| inventory-service | Stock | PostgreSQL | 8082 |
| notification-service | Notifications | MongoDB | 8083 |

## Status

🟡 Under construction — Phase 0 (setup). See progress in [`PLAN.md`](PLAN.md).
