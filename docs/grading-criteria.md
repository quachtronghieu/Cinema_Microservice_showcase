# CinemaManagementMicroService - Agent Context

## Project goal

Transform the existing cinema-management system into a demonstrable Spring Boot microservices solution that satisfies the MSS301 grading criteria. Preserve working business behaviour while making changes incrementally and verifiably.

The GitHub repository is private: `TuongVy-dev/CinemaManagementMicroService`.

## Current codebase

- `Cinema_Management/`: existing Spring Boot 3.5.4 / Java 21 backend. It is currently a monolith using Spring MVC, Spring Data JPA, Spring Security, SQL Server, Thymeleaf, and REST controllers.
- `frontend/`: React 19 + Vite frontend. Validate with `npm run lint` and `npm run build` from this directory.
- `Cinema_Management1.sql`: current database script/reference.
- Existing TODO files record known application bugs. Do not discard them while restructuring.

## Non-negotiable deliverables (100%)

### 1. Architecture and design - 35%

- Split functionality by **business domain**, not by technical layer. Define each service's bounded responsibility and public API before extracting code.
- Each service owns a separate database/schema and must not query another service's tables directly.
- Implement complete CRUD for each service where the domain requires it.
- Preserve and test business rules. A critical rule is preventing double-booking / double-selling the same seat for the same showtime.
- Use request/response DTOs; validate incoming DTOs and return consistent, useful errors through a global exception handler in every API service.

### 2. Spring Cloud - 35%

- Service Discovery.
- API Gateway as the client-facing entry point.
- Config Server with externalised service configuration; secrets must remain outside Git.
- Circuit breakers and clear fallback behaviour for remote calls.

### 3. Deployment - 20%

- A Dockerfile for every runnable service.
- All services must run end-to-end on a Docker network.
- Deploy a runnable production environment using an appropriate free tier (for example Render, Vercel, or Google Cloud). Document the deployed URLs and any demo constraints.

### 4. Security - 10%

- Use Spring Security with JWT or OAuth2.
- Enforce role-based access control (RBAC) both at gateway and service boundaries where appropriate.
- Validate tokens at the gateway; services must not rely solely on the frontend for authorisation.
- Store passwords with BCrypt only; never commit plaintext credentials, JWT secrets, or production configuration.

## Bonus targets (each 10%)

- Docker Compose starts the entire system, including databases and initialization scripts, with one command.
- Distributed tracing via Micrometer/OpenTelemetry plus Actuator health endpoints.
- CI/CD pipeline, preferably GitHub Actions.
- Monitoring with Prometheus and Grafana dashboards.
- Centralized logging with the ELK stack.

## Required working approach

1. Inspect the existing monolith and frontend before moving a domain. Write or update an architecture document describing service boundaries, data ownership, API contracts, and migration order.
2. Keep commits focused and leave the repository buildable after each meaningful milestone.
3. Implement invariants in the backend/database transaction layer, not only in the UI. For seat booking, use an appropriate uniqueness/locking strategy and test concurrent or repeated requests.
4. Use environment variables or ignored local configuration for credentials. Commit safe `.env.example` / configuration templates instead.
5. Prefer explicit error contracts, HTTP status codes, input validation, health checks, and logs that make the demo diagnosable.
6. Before declaring a task complete, run the relevant backend tests/build and frontend lint/build. Report commands that could not be run and why.
7. Do not claim an integration, deployment, test, or security property exists unless it has been implemented and verified.

## Acceptance checklist

- [ ] Domain-service boundaries and database ownership documented.
- [ ] All service APIs have DTO validation and global error handling.
- [ ] Double-booking is prevented and covered by tests.
- [ ] Discovery, Gateway, Config Server, and circuit breaker are operating.
- [ ] Authentication, BCrypt password hashing, JWT/OAuth2, and RBAC are verified.
- [ ] Each service builds and has a Dockerfile.
- [ ] Docker Compose brings up services, databases, and initialization data end-to-end.
- [ ] Health endpoints and a repeatable demo flow work.
- [ ] Production deployment is documented and reachable.
- [ ] Optional observability and CI/CD evidence is included where implemented.

## Guardrails

- The project must remain Spring Boot based.
- The demonstration must run; a non-running demo incurs a severe grading penalty.
- Keep generated build artifacts, dependencies, IDE files, secrets, and local `.env` files out of Git.
- Explain implementation choices clearly enough that the project team can present and maintain the code.
