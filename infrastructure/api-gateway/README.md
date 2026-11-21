# Gateway and Catalog integration

Gateway listens on 8080. Concession and Movie API paths and `/assets/img/concessions/**`, `/assets/img/movies/**` route to `lb://catalog-service`, resolved through Eureka. Catalog registers as `catalog-service` (default port 8082). Identity, Cinema and Booking routes also use `lb://` names. A path must be allowed in SecurityConfiguration as well as routed: `/api/v1/admin/showtimes` and `/api/v1/admin/rooms` require ADMIN, and `/api/customers/profile` requires USER or ADMIN. `/api/admin/staffs` stays denied because no service implements it.

CustomerIdentityHeaderFilter removes any `X-User-Id` sent by the client on every route and sets it from the verified `customerId` claim. Services that read this header are only protected when called through the Gateway; Catalog and Identity read the token directly instead.

Movie reads under `/api/v1/movies/**` and movie images are public; `/api/v1/admin/movies/**` requires ADMIN. Multipart poster uploads pass through unchanged.

Gateway and Catalog each validate RS256 signature, issuer, audience, expiration, not-before and a nonblank subject. Only USER and ADMIN claims become role authorities. `/api/v1/admin/**` requires ADMIN; Concession customer reads require USER or ADMIN. Missing/invalid tokens return 401; insufficient roles return 403. Gateway rejects these requests before forwarding them. Catalog continues to enforce its own authorization when accessed directly.

JWT login/registration/password-reset POST endpoints and public JWKS GET endpoints remain available without authentication. The service-token endpoint is not exposed publicly by the new Gateway security chain. Retired cashier paths are denied and are no longer routed. Health is public without details; other exposed Actuator endpoints require ADMIN. Monitoring scrapers now need an agreed authenticated access configuration; anonymous metrics scraping is not enabled.

## Local startup

1. Reload Maven in IntelliJ after pulling these changes.
2. Run DiscoveryServerApplication on 8761, then IdentityServiceApplication on 8081.
3. Run CatalogApplication with its Git-ignored external database/JWT properties. Wait for CATALOG-SERVICE to appear UP at http://localhost:8761. Registration and client registry refresh can take several seconds; do not assume process startup means discovery is ready.
4. Run/restart ApiGatewayApplication on 8080 so it loads the new code and configuration.
5. Log in through POST http://localhost:8080/api/v1/auth/login and use the returned bearer token for the checks below. Keep token/password values out of shared files and logs.

Gateway local defaults (override through process environment as needed):

| Variable | Default |
|---|---|
| JWT_ISSUER | cinema-identity |
| JWT_JWKS_URI | http://localhost:8081/.well-known/jwks.json |
| JWT_AUDIENCE | cinema-api |
| EUREKA_SERVER_URL | http://localhost:8761/eureka/ |
| GATEWAY_ALLOWED_ORIGINS | http://localhost:3000,http://localhost:5173 |
| TRACING_EXPORT_ENABLED | false |
| ZIPKIN_ENDPOINT | http://localhost:9411/api/v2/spans |
| TRACING_SAMPLING_PROBABILITY | 1.0 |

Issuer is the token's iss value, not automatically an HTTP URL. Public JWKS is obtained directly from Identity, avoiding a bootstrap call through Gateway itself. Configure the actual Flutter web origin through GATEWAY_ALLOWED_ORIGINS; Postman is not subject to browser CORS. Both applications use public keys only. When Identity uses its development-generated signing key, restart Gateway/Catalog after Identity restarts and obtain fresh tokens to avoid cached-key mismatches.

## Live acceptance (record actual results)

Use http://localhost:8080 as the Postman base URL:

| Request / condition | Expected |
|---|---|
| GET /api/v1/admin/concessions, no token | 401, common error JSON |
| Same request with USER-only token | 403, common error JSON |
| Same request with ADMIN token | 200, Catalog DTO |
| GET /api/v1/concessions with USER token | 200, active items only |
| ADMIN create/detail/no-op PUT/changed PUT/stale PUT/deactivate/retry | Existing Concession contract, including versions and 201/200/409/204 |
| GET image path from a Catalog response | 200, packaged image |
| No available Catalog instance or refused connection | 503 DEPENDENCY_UNAVAILABLE, not a successful empty list |

Gateway connect timeout is 2 seconds and response timeout is 3 seconds. No automatic write retries are configured. Eureka may retain an unavailable instance briefly after an unclean stop; a failed connection still returns the common 503 response.

These live checks must be rerun after restarting the changed applications. The earlier user-confirmed 200/401/403 through a static URL route remain historical evidence for the old forwarding configuration, not proof of this discovery/security configuration. To prove the Gateway enforces authorization itself, also check missing/USER/invalid tokens while Catalog is unavailable: rejection must remain 401/403 rather than becoming an upstream 503.

## Automated verification

ADMIN Voucher paths `/api/v1/admin/vouchers/**` route to `lb://booking-service`
under the existing ADMIN security rule. PATCH is allowed by CORS. Use the
[Voucher Postman runbook](../../docs/postman/admin-vouchers.md) after restarting
Booking/Gateway; automated routing fixtures are not proof of live registration.

From the repository root with JDK 21 and Docker available for Catalog SQL tests:

```powershell
.\mvnw.cmd -pl infrastructure/api-gateway,services/catalog-service -am verify
```

GatewaySecurityRoutingTest starts the actual Gateway on a random port with real signed test JWTs, an HTTP JWKS fixture, a controlled service-instance supplier and an HTTP upstream fixture. It exercises the production lb:// route and verifies JWT/role rejection before any upstream request, header/body/query forwarding, statuses, public image routing, missing/refused/slow upstream 503 responses, CORS and Actuator protection. It does not replace a live Eureka registration test or a real Identity-Catalog CRUD run. Reports are under target/surefire-reports; see [verification](../../docs/verification.md) for executed results and limits.
