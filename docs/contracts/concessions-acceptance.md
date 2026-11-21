# Concession contract verification

Contract: [concessions.yaml](concessions.yaml), OpenAPI 3.0.3, version 0.2.0.
Concession CRUD decisions are confirmed by the user and implemented in Catalog. Cross-service producer/consumer review and the quote-to-booking deactivation decision remain pending. See [execution evidence](../verification.md).

## CON-PRICE-01: positive whole VND catalog prices

The user selected whole VND concession catalog prices on 2026-11-03. POST and PUT accept positive integral values, including numerically equivalent inputs `25000` and `25000.00`, with at most 17 integer digits. Non-zero fractional values such as `25000.50` and `0.01` return 400 VALIDATION_ERROR with fieldErrors.price and must not reach the application service. Inputs are not rounded. Missing/non-positive/oversized prices remain invalid.

Java BigDecimal, SQL DECIMAL(19,2), existing migrations and Booking snapshots are unchanged. Booking discount/total rounding is a separate contract. Existing catalog rows with fractional prices must be reviewed and explicitly updated through the versioned ADMIN API before completing acceptance. Do not round or rewrite historical Booking amounts. A response price of `25000.00` remains a valid whole VND value.

Acceptance requires executed validation/HTTP tests, regression of normalized no-op PUT on SQL Server, and the user's live step 6 checks after restarting Catalog. Record each result in verification.md; do not infer live deployment from test fixtures.

## CON-CUSTOMER-01: paginated list and category filters

Status: PASS for the executed scoped automated checks on 2026-11-03. Flutter's catalog_acceptance_test.dart has 19 cases covering the actual DTO/service/repository with an in-memory HTTP adapter and separate controlled widget tests. The default page size remains 20: 21 active UUID items require pages 0 and 1, with no missing/duplicate items, and a DR item on the second page is found by the type filter. Zero items and exactly 20 items stop correctly. Malformed page metadata, duplicate IDs, short pages and totals changing during retrieval are reported as errors instead of successful partial data. Users may retry after a concurrent catalog change; this is not a transaction snapshot across multiple requests.

The SQL Server integration case ConcessionDatabaseIT.customerHttpPaginationReturnsEveryActiveUuidAndHidesInactiveDetail separately checks the real customer HTTP API with 21 active persisted items, stable ID ordering, page sizes 20/1, total count/pages, an empty out-of-range page and an additional inactive item excluded from the results.

## CON-CUSTOMER-02: refresh after deactivation and request errors

Status: PASS in the scoped automated checks. After a successful refresh removes an unavailable item, Flutter removes its stale local cart entry. A failed refresh reports the error and offers Retry even if a previously loaded list remains. Loading, initial failure/retry, empty category and UUID PC/DR filters have executed widget coverage. The real SQL Server HTTP test verifies inactive customer detail is 404 while ADMIN detail remains 200.

## CON-CUSTOMER-03: Android smoke and normal defaults

Status: PASS for two executed integration tests on a Pixel 9a Android emulator: actual device-to-development-Gateway public health, and the production app's catalog/providers/router using a local HTTP fixture for pagination/filtering/inactive/refresh/retry/image requests. This fixture uses no real user credentials and does not write live databases. It does not prove Android Identity login or actual Catalog reads through Gateway; that live authenticated flow remains NOT RUN in this automated batch.

The temporary emulator ran read-only with no snapshot saved and was shut down afterward. A normal debug APK was rebuilt from the normal application entrypoint, replacing the test APK; its Dart asset contains no known catalog-fixture markers. Normal defaults are 20 items/page, Gateway localhost:8080 on Windows and 10.0.2.2:8080 on Android Emulator. Fixture hosts and CHECK_LIVE_GATEWAY are confined to tests. See verification.md for commands, counts, pending live price reconciliation and the unrelated failing movie tests; this is not a full-repository PASS.

## Validate the OpenAPI specification

Run from the repository root in PowerShell. Use a dedicated virtual environment:

```powershell
python -m venv "$env:TEMP\cinema-contracts-venv"
& "$env:TEMP\cinema-contracts-venv\Scripts\python.exe" -m pip install -r docs/contracts/requirements.txt
& "$env:TEMP\cinema-contracts-venv\Scripts\python.exe" -c "from openapi_spec_validator import validate; from openapi_spec_validator.readers import read_from_filename; spec, base_uri = read_from_filename('docs/contracts/concessions.yaml'); validate(spec, base_uri=base_uri); print('OpenAPI 3.0.3 validation: PASS')"
```

Expected: validator exits with code 0. Record its actual result below; this checks the OpenAPI document, not the running API's authorization, transactions or database behavior.

## CON-PUT-01: identical business data preserves version

Status: PASS for the automated Catalog coverage: ConcessionDatabaseIT.identicalNormalizedPutDoesNotWriteOrIncrementAndStaleRequestStillConflicts, inactiveNoOpPutKeepsInactiveAndVersion, and concurrentNoOpPutsBothSucceedWithoutWrite. These ran against SQL Server with the actual service/JPA; the first also uses HTTP handlers. A mock repository alone does not prove the absence of a database write. The acceptance oracle is:

1. Seed an active concession using an approved image. Read its complete DTO and record its persisted version `v`.
2. As ADMIN, PUT the same `type`, `name`, `description`, `price` and image filename, with `version=v`. All values should already be normalized, so trimming or price changes cannot accidentally make the entity dirty.
3. Assert HTTP 200, the same concession ID/business fields, unchanged `active=true`, and response `version=v`.
4. Read through a new transaction/persistence context. Assert the stored version is `v` and business fields are unchanged. Do not rely on an entity remaining in the first-level cache.
5. Repeat the identical PUT with `version=v`. Assert HTTP 200 and unchanged version again.
6. Observe SQL statements for the request's transaction and assert no UPDATE of the concession row. Do not rely solely on response assertions or a mock save call.

Repeat with an inactive fixture: PUT remains permitted, preserves version and leaves `active=false`. Neither case reactivates an item.

Also test semantically equal input after normalization: surrounding whitespace on name/description and price `50000` versus `50000.00` must not trigger an UPDATE or version increment. Compare the resolved approved image path, not the input filename to a stored path. Version must be checked before deciding that no change is needed. Protect the version/no-change decision against concurrent writers; never use a forced version increment for a no-op.

## CON-PUT-02: identical concurrent PUTs

Status: PASS. ConcessionDatabaseIT.concurrentNoOpPutsBothSucceedWithoutWrite uses two separate transactions/connections coordinated with latches.

Both ADMIN requests send business fields equal to the stored normalized data and `version=v`. With no other writer, both return 200 with version `v`, no concession UPDATE, unchanged fields and unchanged active status.

## CON-PUT-03: stale version is rejected even when data matches

Status: PASS. Covered by ConcessionDatabaseIT.identicalNormalizedPutDoesNotWriteOrIncrementAndStaleRequestStillConflicts and the HTTP conflict test.

1. Read an item at version `v`. Successfully change a business field with that version; assert 200 and stored version `v+1`.
2. Send the now-current business fields with the old version `v`.
3. Assert 409 CONCESSION_VERSION_CONFLICT and the common error fields. A fresh read still shows version `v+1` and the changed fields. The data-equality check must not bypass the version conflict.

## Remaining integration acceptance cases

Catalog-only cases below ran in the Web/SQL suites. Gateway now has automated JWT/LoadBalancer coverage with controlled HTTP fixtures; live Eureka acceptance after the new deployment and Booking checks remain NOT RUN. See the status column and [verification report](../verification.md).

| Case | Required result | Actual status |
|---|---|---|
| Create | Backend assigns UUID, active=true and version=0; HTTP 201 and Location | PASS: Web + SQL |
| Two PUTs changing data, same initial version | Exactly one successful write and one conflict; stored version v+1 | PASS: SQL |
| Changing PUT vs DELETE, same initial version | One winning write; version increments once | PASS: SQL |
| No-op PUT vs DELETE, same initial version | No-op before deletion succeeds; no-op after deletion conflicts; only DELETE increments | PASS: SQL |
| DELETE repeated with old version | Inactive retry succeeds without write/version change | PASS: SQL service + HTTP response mapping |
| Concurrent DELETEs | Success after state re-evaluation; one deactivation increment | PASS: SQL |
| Missing/negative/malformed DELETE version | HTTP 400 | PASS: Web |
| Missing ID | Domain 404 | PASS: service; inactive detail HTTP 404 also covered |
| Missing/invalid token | HTTP 401 with common error format | PASS: Catalog Web with real decoder |
| USER calls admin API | HTTP 403 at Catalog and Gateway | PASS: Catalog and Gateway automated tests; live static-route GET also user-confirmed. Live Eureka-route deployment NOT RUN after the new code |
| Customer reads inactive detail | HTTP 404; customer lists omit the item | PASS: Web + SQL |
| Existing booking after Catalog update/deactivation | Snapshot and payment deadline unchanged | NOT RUN: Booking implementation deferred |

Before integration acceptance, agree the outcome when Catalog deactivation occurs between a quote and booking creation. The 60-second quote proposal is [PENDING REVIEW](catalog-booking-quote-proposal.md), not an accepted Booking policy.

## Execution evidence

Full OpenAPI validation after the no-op version policy change: PASS, exit code 0, using openapi-spec-validator 0.7.2
with the document URI supplied for reference resolution. Actual output:

```text
OpenAPI 3.0.3 validation: PASS
```

The validation package was installed into a dedicated temporary directory for
this run and loaded using PYTHONPATH. The virtual-environment command above calls
the same validator API. The updated contract passed revalidation.

Runtime evidence: 51 unit/Web tests and 14 SQL Server integration cases executed with zero failures/errors/skips. See ../verification.md for exact commands, scope and NOT RUN items.
