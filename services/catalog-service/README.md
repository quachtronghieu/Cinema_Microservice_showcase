# Catalog service

Catalog owns Concession, Movie and Watchlist in catalog_db, not yet the whole domain. The contracts are [Concession](../../docs/contracts/concessions.yaml) and [Movie](../../docs/contracts/movies.yaml); Watchlist has no contract yet. Catalog registers with Eureka and Gateway routes its requests through lb://catalog-service; both applications verify JWTs. Rating, Config Server integration, legacy adapters and the quote protocol remain outside these modules. See the [Gateway runbook](../../infrastructure/api-gateway/README.md) for startup and live acceptance checks.

## Build and tests

Use JDK 21 and the repository Maven Wrapper (currently Maven 3.9.16) from the repository root:

```powershell
.\mvnw.cmd -pl services/catalog-service -am verify
```

Surefire runs validation, application-branch and MockMvc tests. HTTP tests use actual RSA-signed JWTs and a public test key, with a mocked application service. Failsafe runs ConcessionDatabaseIT on a disposable SQL Server 2022 CU20 container, using the real service, Flyway, JPA and HTTP handlers. No production/local SQL Server databases are touched by the tests. Docker must be running and able to pull the SQL Server and Testcontainers images. API 1.44 is configured for docker-java in tests for compatibility with Docker Desktop 29.

If Docker is unavailable, Testcontainers marks the DB tests skipped; record these as NOT RUN rather than database PASS. Inspect target/surefire-reports and target/failsafe-reports and [verification](../../docs/verification.md). Unit/mock results alone do not establish SQL Server locking.

## Run locally

Provision catalog_db and the separate users described in [migration](../../docs/migration.md). Populate the environment variables listed in [.env.example](.env.example); Spring does not read that file automatically. Keep passwords outside Git.

For IntelliJ, an alternative is the root [catalog-local.properties.example](../../catalog-local.properties.example). Copy it to catalog-local.properties (Git-ignored), fill local credentials, set the working directory to the repository root, and put --spring.config.additional-location=file:./catalog-local.properties in Program arguments. Enter this in Program arguments, not Environment variables. Both datasource and Flyway URLs are explicitly configured in that file, avoiding semicolon parsing in the IDE environment editor. Real Identity/JWKS remains necessary for authenticated CRUD.

```powershell
.\mvnw.cmd -pl services/catalog-service -am package
java -jar services/catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar
```

The local service port is 8082. In deployment, clients use a configured Gateway URL. The process requires a real issuer/JWKS configuration and validates RS256 signature, issuer, audience, expiration and not-before. It accepts only USER/ADMIN role authorities. Public key material comes from JWKS; no signing key is stored here. Actuator only exposes health without details. Session/form login is not enabled.

ADMIN endpoints are /api/v1/admin/concessions; USER/ADMIN read-only endpoints are /api/v1/concessions. Lists use page starting at zero and size at most 100. Customer lists force active=true. Admin list can filter type and active. Request bodies reject unknown/server-owned fields and invalid validation input.

PUT and DELETE use the same pessimistic row lock in a local transaction plus JPA versioning. Check version before no-op detection. Equal normalized data does not dirty the entity or issue an UPDATE. Changed data flushes before returning the incremented version. Inactive DELETE retries do not write or increment again.

Images use a packaged filename allowlist and classpath streams, with the original ten image assets bundled into the JAR. POST and PUT require filenames, not URLs/paths. Response images are public /assets/img/concessions/... paths routed by Gateway through Catalog discovery. There is no upload API.

## Movie module

Guests can read `/api/v1/movies` without a token; a token that is sent must still be valid. ADMIN endpoints are `/api/v1/admin/movies`. `search` is a case-insensitive title match on both. Customer lists force active=true and take `status=now-showing` or `coming-soon`, where "today" follows CATALOG_BUSINESS_ZONE (default Asia/Ho_Chi_Minh), because release dates are calendar days and a container's clock is usually UTC.

Create and update are multipart/form-data, as in the monolith. PUT and DELETE follow the same locking and version rules as Concession. DELETE is a soft delete that frees the title for a new active movie.

Posters are at most 5 MB and must be JPEG, PNG or WEBP, identified by file signature. Uploads are stored in dbo.Movie_Image in the same transaction as the movie, so every instance can serve them and a failed write leaves nothing behind. They are served from `/assets/img/movies/uploads/`. Legacy posters ship inside the JAR under `/assets/img/movies/`. See [migration](../../docs/migration.md#movie-migration) for what differs from the monolith.

Open Showtimes for a movie is served by cinema-service, not by Catalog. cinema-service reads movie metadata from the public `GET /api/v1/movies/{id}`; the internal metadata API from AGENTS section 18.2 does not exist yet.

## Runtime settings

The X-Trace-Id header and the error `traceId` carry the distributed trace ID. Span export to Zipkin is off unless TRACING_EXPORT_ENABLED=true; see [monitoring](../../monitoring/README.md#4-distributed-tracing-zipkin).

EUREKA_SERVER_URL defaults to http://localhost:8761/eureka/ for local development. Override it for containers/deployment. Database integration tests explicitly disable Eureka to avoid registering disposable fixtures in a developer's registry; they still use real SQL Server transactions.

## Integration status

The quote proposal is [PENDING REVIEW](../../docs/contracts/catalog-booking-quote-proposal.md): proposed TTL 60 seconds, no new quote for inactive items, existing unexpired quotes remain usable. Booking has not approved it. No quote issuance or Booking acceptance behavior is implemented here. Integration acceptance waits for both contracts, both implementations and their tests.
