# CLAUDE.md — orders-microservices

> Project instructions for Claude Code. Read on resume. The work plan and current
> status live in `PLAN.md`. The technical architecture, in `docs/ARCHITECTURE.md`.

## ⚠️ Working mode: PROFESOR (active)

This project is built with the **`/profesor`** skill active. That means:

- **The user (Ciro) writes ALL the project's code.** Claude does NOT edit, write or
  create codebase files (no `Edit`/`Write`/`NotebookEdit` on `*-service/`, `api-gateway/`,
  `discovery-server/`, etc.). Zero `git commit`.
- Claude acts as a **mentor**: explains the concept, assigns a concrete task, waits for
  Ciro to write it, and reviews with clear feedback (what's right, what to adjust and **why**).
- **Allowed exception:** documentation/planning files (`CLAUDE.md`, `PLAN.md`,
  `docs/**`, `README.md`) Claude may write — they aren't code Ciro needs to learn.
- Example snippets in chat are allowed as reference; Ciro adapts and rewrites them.
- Language: **English everywhere** — chat, code, commits, errors, docs.
- Exit the mode: Ciro says `/salir-profesor` or "now you do it".

To start/continue, also read the personal skills `programar-spring-boot`,
`arrancar-spring-boot` and `java-springboot` (Ciro's conventions and best practices).

## What this project is

SaaS platform for **order management for local businesses** (working name: **OrderFlow**).
**Event-driven microservices** architecture. Dual goal:

1. **Learning** — for Ciro to master microservices for interviews (e.g. the Java role at Accenture:
   Spring Cloud, RabbitMQ, microservices, NoSQL, cloud).
2. **Real product** — base for a SaaS sellable to local businesses (Mar del Plata and around),
   connected to his current business of landings for local businesses.

See business vision and full roadmap in `PLAN.md`.

## Stack

- **Java 21** (LTS) · **Spring Boot 3.5.x** · **Maven**
- **Spring Cloud 2025.0.x** (Eureka, Gateway, OpenFeign, Resilience4j) — version matching Boot 3.5
- **PostgreSQL** (order, inventory) · **MongoDB** (notification) — both via Docker
- **RabbitMQ** (Spring AMQP) — asynchronous messaging, via Docker
- **springdoc-openapi** (Swagger UI) · **Spring Boot Actuator** (health/metrics)
- **Testcontainers** + JUnit 5 (integration tests against a real ephemeral DB)
- **Docker** + **Docker Compose** (local orchestration)

> Dependencies are added when generating each service via IntelliJ's **New Project → Spring Boot**
> wizard (the built-in Spring Initializr), not by hand.

## Repo structure

```
orders-microservices/
├── CLAUDE.md                  ← this file
├── PLAN.md                    ← vision, phased roadmap, CURRENT STATUS
├── README.md                  ← quick entry point
├── docs/
│   └── ARCHITECTURE.md        ← diagram, services, ports, decisions (ADR-lite)
├── discovery-server/          ← Eureka (Phase 2)
├── api-gateway/               ← Spring Cloud Gateway (Phase 4)
├── order-service/             ← orders + Postgres (Phase 1)
├── inventory-service/         ← stock + Postgres (Phase 3)
├── notification-service/      ← notifications + MongoDB (Phase 6)
└── docker-compose.yml         ← brings everything up (Phase 7)
```

Each `*-service/` and `api-gateway/`, `discovery-server/` is an **independent Spring Boot project**
(its own `pom.xml`, its own `.jar`, its own port).

## Code conventions (best practices — "impeccable")

Each service follows **layered architecture** with one clear responsibility per class (SOLID):

- `controller/` → HTTP only (receives request, returns response). No business logic.
- `service/` → business logic. Interface + impl when it adds value.
- `repository/` → data access (Spring Data).
- `dto/` → API input/output objects. **Never** expose JPA entities directly.
- `mapper/` → entity ⇄ DTO. **Convention: MapStruct** (`componentModel = "spring"`); for bidirectional
  relationships use `@AfterMapping` to sync the owning-side back-reference (see `order-service`).
- `model/` or `entity/` → domain/persistence entities.
- `config/` → configuration (beans, OpenAPI, etc.).
- `exception/` → own exceptions + global `@RestControllerAdvice`.

Cross-cutting rules:

- **DTOs + Bean Validation** (`@Valid`, `@NotNull`, etc.) on every input.
- **Global error handling** with `@RestControllerAdvice` returning **`ProblemDetail`** (RFC 7807).
- **Swagger/OpenAPI** documented in every service (springdoc).
- **Externalized config**: `application.yml` + profiles + environment variables. **Zero hardcoded secrets.**
- **Tests**: unit (service) + integration (Testcontainers against the real DB). No merging without green tests.
- **Actuator** exposed (`/actuator/health`) in every service.
- **Human-style comments**: explain the *why* (decision/context), not the what. In English, clear.
- **Logs** with SLF4J, correct levels. Never `System.out.println` in production code.

## Commands

```bash
# Bring up local infra (Postgres, RabbitMQ, Mongo) — from the root, once the compose exists
docker compose up -d postgres rabbitmq mongo

# Run a service (from its folder)
./mvnw spring-boot:run

# Tests of a service
./mvnw test

# Build the jar
./mvnw clean package
```

Assigned ports → see `docs/ARCHITECTURE.md`.

## Git workflow

**Layout:** monorepo — a single git repo at `orders-microservices/` holding all services.
The GitHub remote is `origin`. (No repo-per-service; can split later if it ever ships.)

**Model:** GitHub Flow. `main` always works. Every change happens on a short-lived branch,
then merges back to `main` via PR. No GitFlow (`develop`/`release`/`hotfix`) — overkill solo.

**Branching unit:** **one branch per phase** (each `PLAN.md` phase is a self-contained chunk).
Small commits live *inside* the branch (1 class / 1 endpoint → commit); the whole phase merges
as one PR. Per-phase, not per-tiny-feature.

```
main
 ├── feat/phase-1-order-service
 ├── feat/phase-2-eureka
 └── ...
```

**The loop (Ciro runs all git commands):**

```bash
git switch -c feat/phase-1-order-service   # branch off main
# ... work, many small commits ...
git push -u origin feat/phase-1-order-service
# open PR on GitHub, re-read the full diff, merge
git switch main && git pull                 # pull the merged code
git branch -d feat/phase-1-order-service    # delete the merged branch
```

Why a PR even solo: forces a full-diff re-read before `main`, and it's the exact workflow asked
about in interviews. **Branch naming:** `feat/...`, `fix/...`, `docs/...` — lowercase, hyphens, short.

## Ciro's rules (global, reminder)

- **Git is ALWAYS done by Ciro**: commits, push, merge, PRs. Claude prepares the change and hands over
  the ready commit message. Claude **never** runs `git add/commit/push`.
- **Incremental work**: one piece at a time (1 class / 1 endpoint / 1 config). Show and stop.
  Ciro reviews and commits. Don't batch several files without an explicit request.
- Simplest solution first. No over-engineering. One solution, not three.
- Subagents (`Task`/`Agent`) **forbidden**. Work on the main thread with grep/rg/Read.
