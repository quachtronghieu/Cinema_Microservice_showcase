# Central logs with ELK (task 8)

Run on 2026-11-09 on branch NganNDT, uncommitted, from a scratch copy of the code. No local database was read or written.

`mvnw verify` on booking-service after adding the seat hold event logs: 229 tests (80 unit/web, 149 SQL Server on Testcontainers), no failures, none skipped. The new SeatHoldIT test checks the created, rejected, released and expired events and that a rejected hold is not logged as created. After that full run the field name `event` became `event.action`, because `event` is an object in ECS; only the new log test was rerun on that last change. With `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` the same test printed JSON containing `service.name`, `traceId`, `event.action`, `holdId`, `showtimeId` and `customerId`.

The stack in `docker-compose.elk.yml` (Elasticsearch, Kibana and Filebeat 8.17.4) was started on Docker with 5.8 GB. Elasticsearch used about 1 GB, Kibana 0.5 GB, Filebeat 0.15 GB; ports 9200 and 5601 listened only on 127.0.0.1; Kibana status returned 200. Filebeat ingested 5 real JSON lines from that test and dropped a sixth test line holding a reset link; there were no parse errors. Exact searches on `service.name` and `event.action` matched those lines. Searches on `holdId`, `traceId`, `showtimeId` and `customerId` were checked on a second start with one sample line in the same format, and each returned it. The stack and its test data were removed afterwards.

Acceptance on 2026-11-09 by the user on the local stack, code at `5b82203` plus the uncommitted Task 8 changes, services started from IntelliJ on Java 24 (the project standard is 21) against the original databases with file logging on. Identity Flyway applied V3 to `identity_db`. Through the Gateway: a new showtime in room 3 (MV000016, next day 19:00), Open Sales gave 80 AVAILABLE seats in Booking, a hold of 2 seats gave 78 AVAILABLE and 2 HELD, the release returned 80 AVAILABLE. The Booking log file held `SEAT_HOLD_CREATED` and `SEAT_HOLD_RELEASED` with the IDs.

Elasticsearch received logs from all 6 applications with no parse errors. Exact searches: `holdId` 2 documents (created, released), `showtimeId` 2, each `traceId` 1, `service.name` booking-service 73. Kibana Discover on data view `filebeat-*` showed the same holdId, showtimeId, traceIds and all 6 service names (user screenshots, kept outside the repository). No document contained a reset link, `Bearer` or `Authorization`, and the passwords used in the run were in no log file.

This acceptance ran before the distributed tracing commit, so each traceId covered one service. Identity logs `Using generated security password` at startup, and those 2 lines reached Elasticsearch before `elk/filebeat.yml` was changed to drop them; they stay there until the ELK data is removed.

After merging main with distributed tracing (`135b860`), run on 2026-11-09 from a scratch copy on disposable Testcontainers SQL Server, no local database read or written: `mvnw verify` on all six modules had 1 failure of 543 tests and none skipped. RoomStatusIT expected the audit row to keep the `X-Trace-Id` request header, while the filter now prefers the distributed trace ID as intended. The test now compares the audit row with the `X-Trace-Id` of the response. Cinema rerun: 47 tests, no failures, none skipped. Totals from the full run: api-gateway 86, identity-service 9, catalog-service 169, booking-service 232, cinema-service 47. A trace crossing services has not been viewed in Kibana; that it stays one trace across services comes from the live check in the tracing section of this file.

# Demo dataset, backup/restore and log review (task 7, K-03)

Run on 2026-11-07 on branch NganNDT, uncommitted, in a disposable container (`mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04`, no host port, removed afterwards). No local database was read or written.

Setup: the 4 databases were created empty and every migration file of the 4 services was applied in order with `sqlcmd -I` (not through Flyway, so `flyway_schema_history` was an empty stub). Seeds 01, 02, 03 and 06 ran without error. A small fixture was added in booking_db: an ACTIVE hold on 2 seats, a PAID booking with 2 seats, 2 concession items, a 10% voucher, a payment and a checked-in ticket, and a CANCELLED booking with a FAILED payment.

| Step | Result |
|---|---|
| `04_reconcile.sql`, originals | 0 failed ERROR checks out of 27 checks; 2 WARN as expected: I06 `staff1` still holds CASHIER_STAFF, B15 concession `CC000001` not in catalog (the seed has no concessions). Row counts match the README table: Staff 2, Customer 9, Movie 13, Room/Template/Template_Seat 4/4/243 with room 901, Showtime 2, Sales_Snapshot 2, Showtime_Seat 155 |
| `01_backup_4db.sql` | 4 `.bak` files written, `RESTORE VERIFYONLY` valid for each |
| `02_restore_as_copy.sql` | 4 `*_restore` databases ONLINE with new file names; a second run stopped with error 51513 and restored nothing; the unedited placeholder tag is refused |
| `03_compare_copy.sql` | 34 tables compared, 0 mismatches |
| `04_reconcile.sql`, copies | Output identical to the originals apart from the database names |
| Fault injection on the copies | Broken data: booking PAID set to CANCELLED, total changed by 1, ActiveHold deleted, overlapping showtime with an unknown movie, all customer logins disabled. 03 flagged exactly the 4 changed tables; 04 failed B06, B07, B11, B12, C02, I05 and warned C03 |

Two bugs were found and fixed during the run: 02 built the `RESTORE FILELISTONLY` command inside `EXEC (...)` (moved into a variable) and declared a nullable column as SYSNAME; 04 had a collation conflict in K01. NOT RUN at that time: the scripts on the user's local SQL Server, a service started against the restored copies, and the hold flow on them.

Reported by the user on 2026-11-08, output not reviewed here: the demo account step and the backup and restore on the local SQL Server are done, and identity-service ran against its restored copy with the app account. A migrator check on Identity failed; the cause was not reviewed here. Identity's application.yml declares no Flyway account, and the user's local Identity properties have no `spring.flyway.*` keys (only deploy/compose.demo.yaml sets `SPRING_FLYWAY_USER`). The other services on the copies and the hold flow remain to be accepted.

Rebuild from scratch, run on 2026-11-08 on branch NganNDT, uncommitted, on a disposable SQL Server 2022 CU20 container (127.0.0.1:14330, removed afterwards). No local database was read or written; the jars were built from a scratch copy of the code and the 4 services ran on ports 18081-18084 with Eureka disabled, then were stopped.

| Step | Result |
|---|---|
| `00_provision_4db.sql` | 4 databases ONLINE; a second run stopped with error 51541 |
| Flyway through the migrator accounts | Identity 2, Catalog 5, Cinema 2, Booking 1 migrations applied, all successful, `installed_by` is the service's own `*_migrator`. Identity received its account through `SPRING_FLYWAY_USER`/`SPRING_FLYWAY_PASSWORD`, with no change to its application.yml. The 4 services started with `ddl-auto: validate` on their `*_app` accounts |
| Seeds 01, 02, 03 (3 rooms, D-06) | Exit 0 |
| Row counts | Staff 2, Customer 9, Movie 13, Template 3, Room 3, Template_Seat 235, Showtime 2, Sales_Snapshot 2, Showtime_Seat 155: same as the README table |
| `04_reconcile.sql` | 0 failed ERROR checks; 1 WARN, I06 (`staff1` still holds CASHIER_STAFF) |
| API, direct to the services | Register 201, login 200 with `roles=[USER]` and a UUID `customerId`, `/auth/me` returns that ID. SW00023: seat map 75, availability 75. SW00024: 80 and 80. Hold without a token 401; hold of 2 seats 201, then 73 AVAILABLE and 2 HELD; release 204, then 75 AVAILABLE |

Not covered by this run: the Gateway path, and services started against the restored copies on the local SQL Server.

Task 7 acceptance on the local SQL Server, reported by the user on 2026-11-08 (only the first two results below were pasted; the rest was confirmed as matching expectations, output not reviewed here): `03_compare_copy.sql` compared 34 tables with 0 mismatches; `04_reconcile.sql` gave `failed_error_checks = 0` on the originals and on the copies, with the same row counts, status counts and money totals. With Identity, Cinema and Booking pointed at the `*_restore` copies, the seat map, availability, seat hold and release worked through the Gateway. A first attempt created no hold because the Gateway was not running. Run configurations must be pointed back at the original databases afterwards. Identity needs `spring.jpa.properties.hibernate.default_catalog` overridden as well, because its application.yml fixes the catalog to `identity_db`. The K-03 log review was a code read only (log calls, logging config, Authorization header handling), not an inspection of real log output.

Decision D-05 (room status, one-month window) is recorded in the scope and in rooms.yaml; the code and RoomStatusIT arrived on this branch with the merge of main on 2026-11-08.

Merge of main into NganNDT (`3a22d42`), run on 2026-11-08 from a scratch copy of the commit, on disposable Testcontainers SQL Server. `booking-it.properties` was not in the copy, so no local database was read or written. `mvnw verify` on four modules: api-gateway 86, identity-service 7, booking-service 228 tests, no failures, none skipped. cinema-service had 2 errors of 43, both in ShowtimeDatabaseIT: Open Sales now requires the real movie title (`67c61ac`) and the mocked CatalogMovieClient returned none. The test now stubs the title, production code unchanged. Cinema rerun: 43 tests (28 unit/web, 15 SQL Server), no failures, 2 skipped. The two skipped RoomStatusIT tests skip themselves from 22:00 local time and the run was at about 23:40, so they did not execute on this code.

# Cinema rooms, Flyway and security verification

Executed on 2026-11-06 on branch NganNDT, uncommitted, with JDK 21.0.12 and Docker 29.8.2 on disposable SQL Server 2022 CU20 containers. No local database was read or written by these checks.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd -B -pl services/cinema-service,infrastructure/api-gateway -am verify
```

| Check | Result |
|---|---|
| cinema-service | PASS: 20 unit/web tests (RoomWebTest 13, PublicShowtimeRestControllerTest 5, EffectiveEndTest 2) and 11 SQL Server tests (RoomStatusIT 8, ShowtimeDatabaseIT 3), none skipped. Flyway V1/V2 applied and Hibernate validation passed |
| api-gateway | PASS: 74 tests, none skipped |
| OpenAPI rooms.yaml | PASS with openapi-spec-validator |
| Local migration script 05 | PASS on a disposable SQL Server: an old-shaped database with rooms 1, 2 and R1 copied rooms 1 and 2 with their templates, seats, showtimes and showtime seats; R1 and its showtime were listed as skipped; status Active became ACTIVE and a missing sales status became OPEN; the next new room got ID 3; a second run was refused |

RoomStatusIT covers list paging and the 101 size limit, detail, template, 404 for 999 and R1, a change with an audit row (actor, old and new status, traceId), a repeated change with no new audit row, 409 for an upcoming showtime in DRAFT, OPEN or CLOSED, a showtime three months ahead, a running showtime and one from yesterday still running past midnight, an ended showtime not blocking, INACTIVE refusing create, move and Open Sales, and 8 rounds of a status change racing with a showtime creation where exactly one side wins. Two RoomStatusIT tests skip themselves between 22:00 and 04:00 local time because their fixtures must stay on today's date; they ran in this execution.

Showtime ID fix (scope B-03), rerun after it: `.\mvnw.cmd -B -pl services/cinema-service -am verify` exited 0 with 33 tests (20 unit/web, 13 SQL Server, none skipped). New showtimes and showtime seats get lowercase UUIDs; with a legacy SW00024 row present, two new showtimes got distinct UUIDs and SW00024 with its HS200155 seat stayed unchanged; saving a new showtime with an existing ID now fails on the primary key instead of overwriting the row.

Live check on 2026-11-06 by the user, through the Gateway on the local cinema_db after its move to Flyway: ADMIN list (3 rooms, 75/80/80 seats, page 0 of 1), room 1 template with 75 seats, 404 for R1, room 3 to INACTIVE twice with 200, 409 ROOM_HAS_UPCOMING_SHOWTIMES for room 1 naming SW00023, 400 for CLOSED, 409 ROOM_INACTIVE when creating a showtime in the inactive room 3, room 3 back to ACTIVE, 403 for a USER on list and status change, and 401 when calling cinema-service directly without a token. A first USER attempt returned 401 because that token was older than its 15-minute lifetime; a fresh login gave 403.

Confirmed afterwards: Room_Status_Audit has exactly two rows for room 3 (ACTIVE to INACTIVE, INACTIVE to ACTIVE) with actor ST000001 and a traceId; the repeated request added none. Script 05 on the user's data copied Template 3 of 4, Template_Seat 235 of 235, Room 3 of 4, Showtime 2 of 2 and Showtime_Seat 0 of 0, skipping only room R1; rooms 1 to 3 have 75, 80 and 80 seats.

Finding: the traceId stored in the audit and in error bodies is generated by cinema-service, because the Gateway sets X-Trace-Id only on its response and does not pass it downstream. It does not match the Gateway's trace ID. Scope item K-01 tracks this.

Two showtimes created through the API in room 3 got distinct UUIDs, and SW00023 still starts at 10:00. Task 1 and B-03 are accepted on the local stack.

# Seat map, availability and seat holds verification

Executed on 2026-11-05 on branch NganNDT, uncommitted, with JDK 21.0.12 and Docker 29.8.2. All database tests used a disposable SQL Server 2022 CU20 Testcontainers instance. No local database was read or written. This is not acceptance of the full flow.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd -B -pl services/booking-service,services/cinema-service,infrastructure/api-gateway -am verify
cd cinema_booking_app; flutter analyze --no-pub; flutter test --no-pub
```

| Check | Result |
|---|---|
| booking-service | PASS: 29 unit/web and 73 SQL Server tests, including 17 in SeatHoldIT, none skipped |
| cinema-service | PASS: 5 web and 3 SQL Server tests, none skipped |
| api-gateway | PASS: 74 tests, none skipped |
| Flutter | `flutter analyze` no issues; 162 tests passed |
| OpenAPI seat-holds.yaml | PASS with openapi-spec-validator |

The first combined run failed one test: the second-instance test passed its settings at a lower priority than application.yaml. Only that test changed, and booking-service was rerun alone; the cinema and gateway results are from the first run. SeatHoldIT covers 12 customers racing for one seat, overlapping selections in opposite order over 10 rounds with no 503, atomic hold replacement, a failed replacement keeping the old hold, release ownership, consumed holds, the expiry job, closed or started showtimes, the public response hiding owner data, and a hold made in one application context consumed in another started on the same database.

NOT RUN: the app and backend together through a running Gateway and Eureka, the local seeded databases, two separate JVMs, and restart of a running instance. The second instance in SeatHoldIT shares the test JVM.

Live API check on 2026-11-06, run by the user through the Gateway on the local seeded databases, showtime SW00023 with two USER accounts:

| Request | Result |
|---|---|
| Guest GET seat map and availability | 200; Cinema 75 seats, Booking 67 seats |
| USER 1 holds two seats | 201, expiresAt is serverTime plus 5 minutes |
| USER 2 holds the same seats | 409 SEAT_UNAVAILABLE with seatIds of both seats |
| USER 2 releases USER 1's hold | 403 HOLD_NOT_OWNED |
| USER 1 releases | 204; a following availability read shows the seats AVAILABLE |
| No token holds seats | 401 UNAUTHORIZED |

The 8 missing Booking seats (A1 to A7 and B1 of room 1) are local data: Booking was seeded while those template seats still belonged to the earlier test template TP000001. The app shows them as unavailable. The user then reran 03_booking.sql to add them, and confirmed on 2026-11-06 that the 11 Flutter app checks passed against this stack. Still not run: two separate Booking JVMs.

The live hold result recorded for 2026-11-04 below no longer applies: that controller was removed by merge 817982e, and it kept holds in memory.

# Distributed tracing across all services, 2026-11-09

Cinema and Booking now use Micrometer Tracing like Gateway, Identity and Catalog. Cinema's three clients use Spring Boot's `RestClient.Builder`, Booking's Feign client uses `feign-micrometer`, and `traceId` in Cinema, Booking and Identity errors is the Zipkin trace ID. This closes K-01 in [ngan-task-scope.md](ngan-task-scope.md).

`mvnw -B test` exited 0 for all seven reactor projects with 345 unit tests, none failed: Gateway 86, Identity 9, Catalog 135, Cinema 32, Booking 83. The build ran on a copy of the repository so that apps running from IntelliJ were not disturbed.

A live check ran all six applications from packaged jars on separate ports, with Zipkin 3.6.1 and the local SQL Server, through the Gateway:

- Open sales produced one trace of 37 spans across Gateway, Cinema, Catalog, Identity and Booking, with every span linked to its parent. The Cinema and Booking log lines for that call carry the same trace ID.
- Booking preview reached Catalog through Feign and the circuit breaker in the same trace.
- Five error responses from Identity, Cinema and Booking each returned one `X-Trace-Id` header equal to `traceId` in the body.

`docker compose --profile monitoring config` validates the new Zipkin service and tracing variables. The test showtime, its seats and its hold were removed afterwards.

NOT RUN: the Compose stack itself and the Testcontainers integration tests (Docker was not running), and multiple instances of one service.

# Movie, tracing and demo stack verification

## Merge of ThoPD "fix api" (9be0bec), 2026-11-04

That commit moved Cinema, Booking and Watchlist controllers to `/api/v1/...` but changed nothing that calls them. The Gateway routes and security rules, Catalog's security rules, Booking's CinemaClient, Flutter's ApiConfig and the affected tests were moved to the new paths here. Its two old Movie controllers stay deleted. Booking's client now uses `/api/v1/cinema/internal/seats/...`; the Gateway does not route that path and denies it.

`.\mvnw.cmd -B verify` exited 0 for all seven reactor projects with 244 tests, none skipped: Gateway 65, Identity 4, Catalog 135 unit and 34 SQL Server, Cinema 3 unit and 3 SQL Server. booking-service has no tests and was only compiled by Maven. `flutter analyze` reported no issues and all 134 Flutter tests passed.

A live check through Caddy HTTPS, Gateway, Eureka and SQL Server passed all 35 assertions on the new paths, this time with booking-service running. It added a seat hold through Booking and Cinema, a second customer rejected with 409, release by the owner, and 403 for the internal path and the old admin paths.

NOT RUN: the Flutter app against the live backend, the Flutter seat map, and booking creation, payment and tickets. The section below describes the earlier paths.

## Fixes after the ThoPD merge, 2026-11-04

Three problems were fixed: watchlist and profile trusted a client-sent `X-User-Id`, Flutter's Open Showtimes call did not match cinema-service, and the Gateway denied `/api/admin/showtimes`, `/api/admin/rooms` and `/api/customers/profile`. Running the real system then exposed a fourth: every showtime creation returned 500 on SQL Server, because the driver sent `LocalTime` as `datetime` to the overlap query. cinema-service now sets `sendTimeAsDatetime=false`. The new ShowtimeDatabaseIT failed 3 of 3 before that change and passes after it.

`.\mvnw.cmd -B verify` exited 0 for all seven reactor projects with 235 tests, none skipped: Gateway 56, Identity 4, Catalog 135 unit and 34 SQL Server, Cinema 3 unit and 3 SQL Server. booking-service has no tests and was only compiled. `flutter analyze` reported no issues and all 134 Flutter tests passed; no Flutter code changed after that run.

A live check through Caddy HTTPS, Gateway, Eureka and SQL Server passed all 27 assertions, using real tokens and a disposable database. It covered:

- Bob cannot read or delete Alice's watchlist through `X-User-Id`, and a spoofed header does not change whose profile is returned. Anonymous calls get 401.
- `/api/admin/rooms` and `/api/admin/showtimes` return 401 without a token and 403 for USER. ADMIN reaches cinema-service.
- ADMIN creates a showtime for a UUID movie. cinema-service reads the title and duration from Catalog, an overlap returns 400, and seats are generated from the template.
- A guest lists showtimes by movie and date, sorted by start time, with the fields Flutter's DTO casts.

cinema-service was started separately for this check because the demo Compose file does not include it. ADMIN rights came from a direct SQL insert into the disposable database.

NOT RUN: the Flutter app against this live backend, the Flutter seat map, booking-service and the hold-to-payment flow (seat holds returned 503 because Booking was not started), and multiple instances. cinema-service and booking-service have no Spring Security, so ADMIN is enforced only at the Gateway and direct calls to their ports are not blocked.

## Merge with ThoPD, 2026-11-04

The Movie work was combined with origin/ThoPD (9f6908b) on thainh, keeping this Movie module in place of the one in that commit. Watchlist moved onto this module and gained migration V5, because Catalog validates its schema and the table had none. cinema-service now reads movies through GET /api/v1/movies/{id}. The Gateway response timeout went back from 5s to 3s, as AGENTS section 8 specifies and GatewaySecurityRoutingTest requires.

`.mvnw.cmd -B verify` exited 0 for all seven reactor projects, with 46 Gateway tests, 131 Catalog unit tests and 34 Catalog SQL Server tests, none skipped. `flutter analyze` reported no issues and all 133 Flutter tests passed. cinema-service, booking-service and the new Identity profile code have no automated tests, so they were only compiled. NOT RUN after this merge: the live end-to-end check, and any call from cinema-service to Catalog.

## Earlier results, before that merge

Executed on 2026-11-04 on branch feature/catalog-movie, on Windows with Oracle JDK 21.0.12, Docker 29.7.2 and Flutter 3.47.7. This covers Movie, tracing and the demo stack only; it is not full-system acceptance.

## Automated results

`.\mvnw.cmd -B verify` from the repository root exited 0 with BUILD SUCCESS for all five reactor projects. Identity has no automated tests, so it was only compiled.

| Suite | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| GatewaySecurityRoutingTest | 33 | 0 | 0 | 0 |
| GatewayMovieRoutingTest | 13 | 0 | 0 | 0 |
| ConcessionServiceTest | 8 | 0 | 0 | 0 |
| ConcessionValidationTest | 34 | 0 | 0 | 0 |
| ConcessionWebTest | 26 | 0 | 0 | 0 |
| MovieServiceTest | 24 | 0 | 0 | 0 |
| MovieWebTest | 39 | 0 | 0 | 0 |
| ConcessionDatabaseIT (SQL Server 2022 CU20) | 15 | 0 | 0 | 0 |
| MovieDatabaseIT (SQL Server 2022 CU20) | 18 | 0 | 0 | 0 |
| Total | 210 | 0 | 0 | 0 |

MovieDatabaseIT covers concurrent same-title creates, PUT against PUT, PUT against DELETE and DELETE against DELETE, no stray image after a failed create, literal search wildcards, and the staged V3/V4 migration with rollback.

In `cinema_booking_app`, `flutter analyze` reported no issues and `flutter test` passed all 133 tests. On main before this change, 10 tests in movie_discovery_screen_test.dart were already failing because of a stale tab label and an overflowing tab row; both are fixed here. A new flow test exposed a real overflow in the showtime card at phone width, which is also fixed.

`docs/contracts/movies.yaml` passes `redocly lint` with 4 style warnings and no errors.

## End-to-end run through HTTPS

`deploy/compose.demo.yaml` was built and started from clean volumes on localhost, with Caddy's local certificate authority, then removed. A throwaway script made 39 checks through the Gateway over HTTPS and all passed on the final run: HTTP redirect, guest and role rules, registration and login through `lb://identity-service`, poster upload with byte-identical download, rejection of a disguised file and of a file over 5 MB by the real Tomcat limit, duplicate title, list and search, version conflict, soft delete, data and images surviving a Catalog restart, and spans from Gateway with Catalog and with Identity in one Zipkin trace.

The run found two defects that the automated tests had missed, both now fixed: the Actuator mail check marked Identity unhealthy when SMTP was not configured, and clients received X-Trace-Id twice through the Gateway. The second now has a regression test. ADMIN rights for the run were granted by a direct SQL insert into the disposable database, because the demo has no other route to an ADMIN whose password is known.

The Postman collection then ran through Newman against the same stack, with a registered test account as ADMIN: 24 requests and 27 assertions, 0 failed. Newman needs the poster's file path set first because a collection cannot carry a local file.

## NOT RUN

- A public deployment. HTTPS was exercised on localhost with a local certificate only; Let's Encrypt issuance, DNS and real resource use are unverified.
- Open Showtimes against a real backend. At the time of this run cinema-service did not exist; the Flutter flow was tested with a fake repository.
- The Flutter app on a device or emulator against the running stack.
- `docs/migration/catalog_movie_import.sql`. No Cinema_Management1 database was available.
- Multi-instance Catalog. Uploads are stored in the database so instances should agree, but two instances were not run.

# Catalog Concession verification

Automated checks executed on 2026-11-01, with merge checks and Identity-token manual checks on 2026-11-03; initial local manual checks were confirmed on 2026-11-02. This report covers the new Catalog module only; it is not full-system or Catalog-Booking integration acceptance.

## Environment and commands

### Merge verification on 2026-11-03

Merged origin/main at 10433e9 into local HieuQT, preserving Catalog source and migrations. Root Maven configuration includes Discovery, Gateway, Identity and Catalog; Catalog now references the shared parent coordinates while retaining artifact version 0.1.0-SNAPSHOT and JDBC 12.10.1.jre11. Maven Wrapper follows main at 3.9.16.

Executed the root Maven Wrapper verify command with JDK 21 and the existing local Maven cache: BUILD SUCCESS for all five reactor projects. Catalog unit/HTTP tests: 51 executed, 0 failures/errors/skips. Catalog SQL Server integration: NOT RUN on this run because Docker was unavailable (Failsafe reported 13 skipped test methods; parameterized cases were not executed). Earlier database PASS results below remain historical evidence, not a fresh database verification after this merge. Flutter checks and live inter-service integration were NOT RUN. The main branch now supplies Identity/Gateway/Discovery code; earlier notes about their absence describe the pre-merge checkout.

Local merge log: .idea/local-backups/pre-main-20261103/merge-verify.log (Git-ignored). The pre-existing untracked Flutter directory was preserved at .idea/local-backups/pre-main-20261103/cinema_booking_app; the working Flutter directory comes from main. Local credentials and personal notes remain ignored.

Windows, Oracle JDK 21.0.8, Maven Wrapper 3.3.4 / Maven 3.9.9, Spring Boot 3.5.4, Spring Cloud BOM 2025.0.0. SQL Server 2022 CU20 ran in a disposable Testcontainers container on Docker Desktop 29.8.0. Those automated tests did not change the host SQL Server or legacy database. Subsequent local setup and manual checks used the user's catalog_db on the host SQL Server, as recorded below.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -pl services/catalog-service -am verify
.\mvnw.cmd -pl services/catalog-service help:effective-pom '-Doutput=target/effective-pom.xml'
.\mvnw.cmd -pl services/catalog-service dependency:tree '-DoutputFile=target/dependency-tree.txt'
```

The actual run also set maven.repo.local to the existing local Maven cache. No skip-tests flag was used. The verify command exited 0 with BUILD SUCCESS. Raw reports are under services/catalog-service/target/surefire-reports and target/failsafe-reports. Build logs and generated dependency evidence stay in ignored target directories.

## Executed results

### Post-merge rerun on 2026-11-03

The user ran `./mvnw.cmd -pl services/catalog-service -am verify` after starting Docker. The agent inspected the generated XML reports: Surefire files were modified at 11:30:14-11:30:19 local time, and Failsafe files at 11:30:56. The user also supplied the matching reactor summary with BUILD SUCCESS for the parent and Catalog projects.

| Suite | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| ConcessionServiceTest | 8 | 0 | 0 | 0 |
| ConcessionValidationTest | 20 | 0 | 0 | 0 |
| ConcessionWebTest | 23 | 0 | 0 | 0 |
| ConcessionDatabaseIT | 14 | 0 | 0 | 0 |
| Total | 65 | 0 | 0 | 0 |

ConcessionDatabaseIT took 31.184 seconds; the matching failsafe-summary.xml records 14 completed, zero failures/errors/skips and no timeout. Its output contains SQL Server container and successful migration evidence. Executed cases include changed/no-op PUT concurrency, PUT versus DELETE, concurrent/repeated DELETE, no-op SQL write checks, filtering, UUID creation and migration preservation/rollback. SQL Server integration is therefore PASS for this post-merge rerun; the Docker-unavailable merge run above remains a separate historical NOT RUN. Gateway, Flutter and Booking integration were not exercised by this command.

### Original automated run on 2026-11-01

| Check | Actual result | Evidence |
|---|---|---|
| Bean validation | PASS: 20 tests, 0 failures/errors/skips | ConcessionValidationTest |
| Application branches | PASS: 8 tests, 0 failures/errors/skips | ConcessionServiceTest; mocks do not prove locking |
| HTTP/security | PASS: 23 tests, 0 failures/errors/skips | ConcessionWebTest, RSA-signed JWTs and real decoder; mocked service |
| SQL Server integration | PASS: 14 cases, 0 failures/errors/skips | ConcessionDatabaseIT, real Flyway/JPA transactions and separate concurrent connections |
| Package | PASS | catalog-service-0.1.0-SNAPSHOT.jar produced by verify |
| Image packaging | PASS | Checked all 10 manifest entries exist inside the executable JAR |
| OpenAPI 3.0.3 | PASS, exit 0 | openapi-spec-validator 0.7.2 with base URI/reference resolution |

Total automated Java cases: **65 executed, 0 failed, 0 errors, 0 skipped**.

SQL tests include clean catalog_db migration and Hibernate schema validation; staged legacy import preserving IDs/prices and filling type/active/version; unknown category and non-positive price rejection with transactional rollback; UUID creation; active/type filters; normalized no-op PUT with zero observed UPDATEs; inactive no-op PUT; stale-version rejection before equality; repeated/concurrent DELETE; changed PUT contention; concurrent no-op PUT; changing/no-op PUT competing with DELETE; concurrent creates.

HTTP tests verify 201/Location, 204/empty body, validation errors, server-owned/unknown input rejection, numeric enum rejection, strict query boolean filtering, 401 for missing/invalid signature/issuer/audience/expired tokens, and 403 for USER and legacy staff roles on every admin command. These test Catalog directly, not a real Gateway deployment.

Effective package versions verified inside the JAR: Hibernate 6.6.22.Final, Spring Security 6.5.2, Flyway 11.7.2, JDBC 12.10.1.jre11. Tests use Testcontainers 1.21.3. Maven effective POM/dependency tree generation evidence is stored under the service target directory.

During development, SQL Server tests exposed a same-batch new-column reference error and Hibernate lowercasing of the legacy table name. They were fixed and the complete verify suite rerun successfully. The negative migration tests intentionally log migration failure/rollback; their expected failures are assertions within passing tests.

## Local manual checks confirmed on 2026-11-02

These results are based on the startup log supplied by the user and the user's explicit confirmation of the manual checks. They were not independently rerun by the agent and do not add to the 65 automated Java cases above. Local credentials are kept in the Git-ignored catalog-local.properties, loaded through spring.config.additional-location.

| Check | Actual result | Evidence |
|---|---|---|
| Local database and account connectivity | PASS (user-confirmed) | User created catalog_db and confirmed that catalog_app and catalog_migrator can connect and access it |
| Catalog startup | PASS | Supplied log shows JPA EntityManagerFactory initialized, Tomcat listening on 8082 and Started CatalogApplication |
| GET /actuator/health | PASS (user-confirmed) | User confirmed the expected response: status UP |
| Local Flyway migrations and schema | PASS (user-confirmed) | User confirmed versions 1 and 2 have success = 1 in dbo.flyway_schema_history and dbo.Concession includes type, active and version |
| GET /api/v1/admin/concessions without Authorization | PASS (user-confirmed) | User confirmed the manual missing-token check returned 401 Unauthorized |

Startup and account connectivity do not prove least-privilege enforcement, authenticated CRUD, or integration with real Identity tokens. No live Identity USER/ADMIN token tests were performed in these manual checks.

## Direct Identity-Catalog manual checks on 2026-11-03

The user ran these checks in Postman against local Identity (8081) and Catalog (8082), using actual Identity-issued RS256 JWTs. Results below reflect supplied responses and user confirmations, not an independently executed agent test suite. Test item: d32451a6-e786-4c92-aa27-0132c7f7152d. No bearer tokens, passwords or password hashes are recorded here.

Catalog's local issuer was corrected from the earlier placeholder URL to cinema-identity, matching Identity; audience is cinema-api and JWKS is http://localhost:8081/.well-known/jwks.json. These are local settings; environment URLs must be configured for deployment.

| Check | Actual result | Evidence / limit |
|---|---|---|
| Identity JWKS and login | PASS | Public JWKS response supplied; login returned 200 and roles ADMIN/USER; a separate USER-only account was used for authorization checks |
| ADMIN reads Catalog list | PASS | User supplied 200 with an initially empty paginated list after correcting issuer and restarting Catalog |
| USER-only calls admin list | PASS (user-confirmed) | GET /api/v1/admin/concessions returned 403; this manual result does not cover every admin HTTP verb |
| ADMIN creates item and reads list/detail | PASS (user-confirmed) | User supplied the created UUID and confirmed the instructed create/list/detail checks succeeded |
| Identical-data PUT | PASS (user-confirmed) | User subsequently confirmed completion of the outstanding manual check that identical data preserves version. No manual SQL statement inspection was performed; the post-merge ConcessionDatabaseIT rerun separately verifies no-op writes |
| Changed-data PUT | PASS | Supplied DTO has price 55000, active=true, version=1 after submitting version 0 |
| Stale PUT with current business data | PASS for HTTP status (user-confirmed) | User confirmed 409. Exact error code and follow-up persisted values were not separately supplied |
| Active DELETE with stale version | PASS (user-confirmed) | User subsequently confirmed completion of the outstanding manual check: stale DELETE on an active item returns 409 and leaves the item active |
| Deactivation with current version | PASS (user-confirmed) | User confirmed the instructed 204/empty response and subsequent admin detail active=false, version=2 |
| Repeated inactive DELETE with old version | PASS (user-confirmed) | User confirmed the instructed check, explicitly reporting version remained 2 |
| Customer reads inactive item | PASS (user-confirmed) | User confirmed customer detail 404 and list 200 excluding the inactive item, using the instructed USER-only token |

These sequential requests exercise local authentication, selected authorization paths and CRUD behavior. They do not by themselves prove concurrent locking or absence of UPDATE statements on no-op PUT; the post-merge SQL Server rerun above provides separate automated evidence for those behaviors. Gateway routing, Flutter behavior and Catalog-Booking integration remain outside these checks.

## NOT RUN and review gates

### Gateway JWT and Catalog discovery implementation on 2026-11-03

Before this change, the user confirmed admin GET 200, USER-only admin GET 403 and anonymous admin GET 401 through port 8080. That configuration forwarded to a fixed Catalog URL and relied on Catalog for authentication; these are historical manual results, not proof that Gateway verified JWTs.

Implemented Catalog Eureka registration and Gateway `lb://catalog-service` routing for Concession APIs and packaged images. Gateway now validates RS256/issuer/audience/time/subject, maps USER/ADMIN roles, enforces admin access itself and returns the common error shape with traceId. Modern Gateway configuration keys replace migrated keys; Prometheus uses access=read-only. CORS origins are configurable, sensitive Actuator endpoints require ADMIN, retired cashier paths are denied, and missing/refused/slow Catalog dependencies return 503 with 2-second connect/3-second response timeouts. No DB migration changed.

Executed with JDK 21 and the existing Maven cache:

```powershell
.\mvnw.cmd -pl infrastructure/api-gateway,services/catalog-service -am verify
.\mvnw.cmd -pl infrastructure/api-gateway verify
```

Both commands exited 0 with BUILD SUCCESS. The combined run passed Gateway's first 31 cases plus all 65 Catalog cases (51 unit/HTTP and 14 real SQL Server integration cases, zero skipped). After adding refused/slow upstream handling, the final Gateway-only run passed all **33 Gateway cases**, zero failures/errors/skips. Latest affected suites therefore total **98 passing cases** across these runs; the Gateway-only rerun did not rerun Catalog because no Catalog code changed afterward. Gateway and Catalog executable JARs were packaged successfully.

Evidence: infrastructure/api-gateway/target/surefire-reports/TEST-vn.edu.fpt.cinema.gateway.GatewaySecurityRoutingTest.xml; Catalog Surefire/Failsafe XML reports; local ignored logs .idea/local-backups/gateway-integration-verify.log and gateway-final-verify.log. Gateway tests use real HTTP on a random port, actual JWT signatures and HTTP JWKS/upstream fixtures, with a controlled service-instance supplier. They verify production lb:// routing and rejection before forwarding, not live Eureka registration, real Identity login or persisted Catalog CRUD through the new Gateway deployment.

Live discovery and new-Gateway deployment acceptance: **NOT RUN**. Restart Catalog/Gateway, verify CATALOG-SERVICE is UP in Eureka, then execute the [Gateway runbook](../infrastructure/api-gateway/README.md) against all running services. Existing manual direct Identity-Catalog CRUD results remain valid for their dated scope; no new live deployment PASS is inferred from these automated fixtures. Flutter, quote policy and Booking remain separate pending work.

| Item | Status | Reason / next condition |
|---|---|---|
| Real legacy dataset export/import and backup/restore | NOT RUN | No reviewed source export/cutover was authorized or supplied; only disposable fixtures migrated |
| Full SQL account permission verification | NOT RUN | Local catalog_app and catalog_migrator have been provisioned and connectivity/startup confirmed; allowed operations and rejection of unauthorized DDL or cross-database access have not been exhaustively tested. SQL container tests use an isolated admin fixture credential |
| Core direct Catalog manual flow | PASS (user-confirmed) | User confirmed the listed CRUD, no-op/stale-version, deactivation/retry and USER-read checks with actual Identity tokens; the table distinguishes exact supplied responses from confirmations and does not imply exhaustive input/security testing |
| Real Identity JWT/JWKS with direct Catalog access | PASS (user-confirmed) | Local login, JWKS, ADMIN read/CRUD and USER-only checks described above; not full Identity security acceptance |
| Gateway JWT/LoadBalancer behavior | PASS (automated) | 33 executed Gateway HTTP/security/routing/failure cases with signed test JWTs and controlled HTTP/service-instance fixtures, as described above |
| Live Catalog Eureka registration and new Gateway deployment | NOT RUN | Prior user-confirmed static-route 200/401/403 does not verify the newly added discovery and Gateway JWT implementation; restart applications and follow the runbook |
| Two independent Catalog JVMs | NOT RUN | Current DB concurrency tests use separate connections/transactions within one application context |
| Legacy adapter/Flutter integration | NOT RUN | Existing client routes were not changed or cut over |
| Catalog deployment image / full Compose | NOT RUN | Outside this CRUD implementation; Docker was used for the SQL test fixture only |
| Booking historical snapshots/payment preservation | NOT RUN | Booking changes deferred to its owner; no Booking code changed |
| Quote-to-booking deactivation race | PENDING REVIEW; tests NOT RUN | 60-second proposal has not been accepted by Booking; shared contract and implementations are pending |

NOT RUN means unverified, not an assertion that the code is wrong. Re-run and record PASS/FAIL when each dependency is available. Full integration acceptance requires both Catalog and Booking to agree contracts, implement the selected policy and execute their related tests.

## Flutter session-expiry verification — 2026-11-03

Executed `test/core/network/session_expiry_test.dart` using the production Dio interceptors, AccountViewModel, router and providers with an in-memory HTTP adapter. The fixtures contain no real credentials or JWTs and do not change Identity's 900-second token lifetime. ApiClient now accepts optional Dio/CookieJar dependencies so the same interceptor path can run without network or disk storage in these tests; normal application initialization is unchanged.

The first run exposed two failures: a delayed `/me` response could restore an expired account, and a delayed profile read could complete login after expiry. AccountViewModel now invalidates pending authentication results with a session generation when expiry, logout, a new login or disposal occurs. Both regression cases passed after the fix. The old successful-login test fixture was updated to include the now-required accessToken; a separate case verifies that a response missing that token is rejected.

| Check | Result | Evidence / scope |
|---|---|---|
| Flutter analysis | PASS | `flutter analyze --no-pub`: No issues found |
| Session-expiry tests | PASS, 10 cases | Current-token 401 clears bearer/account/profile without retry; 403/500 preserve the session; invalid login does not trigger expiry; stale-token 401 preserves a new token; concurrent 401 expires once; re-login recovers; delayed me/profile responses cannot restore expiry; production router/providers clear catalog/cart and navigate to login |
| Focused authentication and concession regression | PASS, 17 cases | `flutter test --no-pub test/core/network/session_expiry_test.dart test/features/account/data/repositories/account_repository_impl_test.dart test/widget_test.dart --reporter expanded --timeout 30s`, exit 0 |
| Entire Flutter suite | FAIL: 89 passed, 10 failed | `flutter test --no-pub --machine --timeout 30s`, exit 1. All remaining failures are in `test/features/movies/presentation/screens/movie_discovery_screen_test.dart`: horizontal overflow in the tab row and saved-tab expectations using `ĐÃ LƯU` while the screen uses `MỤC YÊU THÍCH`. Movie code/tests were not changed by this session-expiry fix |
| Live app waiting for JWT expiration | NOT RUN | Replaced for this check by deterministic HTTP-401 tests. These establish client behavior after rejection, not cryptographic JWT validation or a live 15-minute wait |
| Decimal concession prices and pagination acceptance | NOT RUN in this change | Separate remaining checks; session tests use one page and an integer price |

The full-suite machine report is local and ignored: `cinema_booking_app/.dart_tool/session-validation/full.jsonl`. Prior Gateway JWT tests cover expired signed tokens in their separately dated scope; no backend tests were rerun for this Flutter-only change. No Booking integration acceptance is implied.

AI assistance for this change: added and executed deterministic session tests, fixed the two reproduced authentication races, and recorded both passing checks and the remaining full-suite failures.

## Whole-VND concession catalog prices — 2026-11-03

User decision: concession catalog prices are positive whole VND values. Completed the authorized automated steps 1–5; live deployment and data reconciliation in step 6 are left to the user. This entry supersedes the earlier pending decimal-price work: fractional catalog prices are rejected instead of introducing decimal display/cart arithmetic. It does not change Booking discount/total precision or historical snapshots.

CreateConcessionRequest and UpdateConcessionRequest strip insignificant trailing zeros, then validate at most 17 integer digits and no non-zero fractional part. Thus `25000`, `25000.00` and integral scientific notation are valid; `25000.50`, `0.01`, zero, negative, missing and oversized values remain invalid. No input is rounded. BigDecimal, DECIMAL(19,2) and the applied V1/V2 migrations are unchanged. The OpenAPI draft version is now 0.2.0, with `multipleOf: 1`, a maximum of 99999999999999999 and matching acceptance documentation.

Executed using JDK 21, the existing Maven cache and Docker Engine 29.8.0:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\mvnw.cmd '-Dmaven.repo.local=C:/Users/Hieu/.m2/repository' -pl services/catalog-service -am --batch-mode --no-transfer-progress verify
```

The final verify invocation exited 0 with BUILD SUCCESS. Surefire/Failsafe XML reports confirm:

| Suite / check | Executed result |
|---|---|
| ConcessionServiceTest | PASS, 8 tests |
| ConcessionValidationTest | PASS, 34 tests |
| ConcessionWebTest | PASS, 26 tests |
| ConcessionDatabaseIT on disposable SQL Server 2022 | PASS, 14 tests |
| Combined Catalog result | PASS, 82 tests, zero failures/errors/skips |
| Complete OpenAPI validation | PASS, openapi-spec-validator 0.7.2, OpenAPI 3.0.3 |
| Price schema examples | PASS, 4 accepted and 6 rejected values, including the 17-digit boundary |
| Restarted live Catalog/Gateway integer-price acceptance | NOT RUN; user performs step 6 |
| Live fractional-row reconciliation, including Decimal Cola | NOT RUN; no local catalog_db rows were read or written by this change |
| Flutter multi-page acceptance and Android smoke test | NOT RUN; remaining work before closing the two Iteration 2 tasks |

The 14 SQL cases include normalized no-op updates, unchanged version, concurrent updates and repeated/concurrent deactivation. This regression result does not mean SQL now forbids fractional values; the database type and existing constraints intentionally remain unchanged. API validation enforces the new-write rule.

Evidence: `services/catalog-service/target/surefire-reports/TEST-*.xml`, `services/catalog-service/target/failsafe-reports/TEST-vn.edu.fpt.cinema.catalog.concession.ConcessionDatabaseIT.xml`, and ignored local log `.idea/local-backups/catalog-whole-vnd-verify.log`. The dedicated OpenAPI validator environment under `.idea/local-backups/contracts-venv` is local and ignored. No production application was restarted, no migration was edited, and no commit/push was performed for this step.

## Iteration 2 customer concession checks — 2026-11-03

Scope: View Concession Catalog / View Concession List and Filter by Category from the latest English Backlog sheet. The user authorized automated pagination, inactive and Android checks and requested normal defaults afterward. This section records the later checks and supersedes the earlier NOT RUN entries for automated multi-page and Android smoke coverage; live authenticated Android acceptance and manual price reconciliation are still separate.

Added catalog_acceptance_test.dart and a shared test-only HTTP fixture. Reproduced and fixed acceptance gaps: invalid pagination metadata/duplicate IDs could silently produce an incomplete or duplicated list; refresh left selected inactive items in the local cart; refresh errors were invisible when old catalog data existed. The client now validates pagination consistency, reports/retries incomplete reads, prunes missing cart items after a successful refresh, and shows refresh errors alongside retained data. No server-side transaction snapshot or Booking quote guarantee is claimed.

Executed results:

| Check | Result | Scope / evidence |
|---|---|---|
| Flutter catalog acceptance | PASS, 19 cases | Defaults, 21 UUID items over two pages of size 20, PC/DR including page 2, empty/exact-size lists, malformed/changing metadata, no partial success on later failure, inactive refresh/detail, stale-request reset, whole-price parsing, loading/error/retry widgets |
| Focused Flutter regression | PASS, 36 cases | Catalog 19 + session expiry 10 + account repository 6 + existing cart widget 1; final command exited 0 |
| Flutter analysis | PASS | flutter analyze --no-pub: No issues found |
| Entire Flutter suite | FAIL: 108 passed, 10 failed | Final machine report; all ten remaining failures are in movie_discovery_screen_test.dart (tab-row overflow and outdated saved-tab text expectations), outside this change |
| SQL Server Catalog verify | PASS, 83 cases, zero failures/errors/skips | 68 unit/HTTP + 15 SQL integration cases; the new HTTP/SQL case checks 21 persisted active UUIDs over two pages, stable ordering and inactive customer 404/admin 200. Maven verify exited 0 with BUILD SUCCESS |
| Android emulator to real Gateway | PASS | Pixel 9a Android emulator reached http://10.0.2.2:8080/actuator/health and received 200/UP |
| Android catalog fixture smoke | PASS | Production app on Pixel 9a with local HTTP fixture: actual network/JSON, 21 items in pages 0/1 of size 20, filters, image request, selected item removed after deactivation, detail 404, refresh error and Retry. No real JWT verification in this fixture |
| Normal Android debug build after test | PASS | flutter build apk --debug --no-pub exited 0; build/app/outputs/flutter-apk/app-debug.apk rebuilt as the normal app. Known test fixture markers absent from assets/flutter_assets/kernel_blob.bin |
| Android real Identity login and authenticated live Catalog read through Gateway | NOT RUN | Fixture smoke plus public Gateway health does not establish this complete authenticated flow |
| Live whole-price deployment/data reconciliation (previous step 6) | NOT RUN / awaiting user confirmation | No live catalog_db data was read or changed. The reported Decimal Cola fractional row still requires the user's explicit versioned update/check if not already done |
| Quote/Booking integration | PENDING REVIEW; related tests NOT RUN | Existing producer/consumer decision remains deferred |

Executed from cinema_booking_app:

```powershell
flutter test --no-pub test/features/customer/catalog_acceptance_test.dart test/core/network/session_expiry_test.dart test/features/account/data/repositories/account_repository_impl_test.dart test/widget_test.dart --reporter expanded --timeout 30s
flutter test --no-pub integration_test/concession_catalog_smoke_test.dart -d emulator-5554 --dart-define=CHECK_LIVE_GATEWAY=true --reporter expanded
flutter build apk --debug --no-pub
flutter analyze --no-pub
flutter test --no-pub --machine --timeout 30s
```

The Android integration command exited 0 with two passed cases; its SDK integration_test dependency is dev-only and recorded in pubspec.lock. No temporary pageSize=1 was introduced. The emulator was started read-only with no snapshot save, then stopped. Normal API defaults now target Gateway (Windows localhost:8080; Android Emulator 10.0.2.2:8080) and production code contains no fixture URL/test flag. The normal APK, SDK config, test logs and emulator logs remain local/ignored; no commit or push was performed.

Local evidence: cinema_booking_app/.dart_tool/session-validation/catalog-final-focused.log, catalog-final-full.jsonl, android-catalog-smoke.log, android-normal-build.log; .idea/local-backups/catalog-customer-acceptance-verify.log; Catalog Surefire/Failsafe XML. The full Flutter suite is not green and the two remaining live checks above must not be marked PASS from fixtures. Backend and Windows/Android fixture coverage can be reviewed independently of deferred Booking work.

AI assistance: implemented the reproduced pagination/refresh fixes, added and ran the scoped/backend/Android checks, restored normal build output and documented their scope and remaining failures.

## ADMIN Voucher contract and entity mapping - 2026-11-07

The user approved docs/contracts/vouchers.yaml. This step completes only the
existing Voucher entity mapping, not ADMIN endpoints or expectedTotal integration.
Voucher now maps voucher_name and a read-only version column without @Version,
exposes DTO getters, and retains the old constructor with code as its name fallback.
The new constructor accepts an explicit voucher name. Native Booking SQL continues
to own version increments. No applied migration or local database was changed.

The first Maven verify attempt passed 29 unit/HTTP tests but failed to initialize
the SQL integration contexts because Docker Engine was not running. This was an
environment error, not a passing SQL test. After starting Docker Desktop, the full
verify command was rerun successfully using a disposable SQL Server 2022 container:

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\mvnw.cmd '-Dmaven.repo.local=C:/Users/Hieu/.m2/repository' -pl services/booking-service -am --batch-mode --no-transfer-progress verify
```

Final exit code 0, BUILD SUCCESS. Surefire/Failsafe XML confirms **105 tests PASS**:
29 unit/HTTP and 76 SQL integration tests, zero failures/errors/skips. Included in
that total are three new VoucherMappingIT cases: named JPA insert/read, compatibility
constructor insert/read, and refresh after native reserve reading updated counters
and version without an extra JPA version increment. Existing booking, quota,
payment, cancellation, ticket and hold suites also passed for their existing scope.

Evidence: services/booking-service/target/surefire-reports/TEST-*.xml,
services/booking-service/target/failsafe-reports/TEST-*.xml and the ignored local log
.idea/local-backups/voucher-mapping-verify.log. No booking-it.properties was present;
tests used the existing Testcontainers path rather than a local SQL database.

OpenAPI validation and nine schema examples passed separately for the five-operation
Voucher contract. Runtime ADMIN CRUD, Gateway Voucher routing and expectedTotal/
fresh-discount race integration remain **NOT RUN** because they are not implemented
by this mapping step. No endpoint/task completion is inferred from these tests.

## ADMIN Voucher request/response DTOs - 2026-11-07

Implemented CreateVoucherRequest, EditVoucherRequest, VoucherResponse and VoucherPage,
plus the PERCENTAGE/FIXED enum and cross-field discount validation. Create normalizes
code/name and insignificant decimal zeros before Bean Validation. PATCH distinguishes
omitted values from explicit JSON null (rejected), rejects unknown/server-owned
fields, rejects empty edits, and preserves false/zero as supplied values. A partial
type/value edit still requires validation against the current DB row in the future
application command; DTO-only checks cannot establish that result.

An additional regression check reproduced Jackson accepting numeric/boolean values
as code/name strings despite allow-coercion-of-scalars=false. A field-scoped string
deserializer now rejects these inputs on create and PATCH without changing other
Booking DTO behavior. Both failing cases passed after the change.

Executed the same Maven command from the mapping section with `test` instead of
`verify`. Final result: exit 0, BUILD SUCCESS; **80 unit/JSON/HTTP tests PASS**,
zero failures/errors/skips, including **51 VoucherDtoTest cases** and 29 existing
Booking cases. Coverage includes missing/invalid fields, code normalization,
percentage/DECIMAL boundaries, null/omitted PATCH semantics, numeric overflow,
wrong JSON types, unknown/server-owned fields and response/page serialization.

Evidence: services/booking-service/target/surefire-reports/TEST-*.xml and ignored
.idea/local-backups/voucher-dto-test.log. SQL tests were not rerun for this DTO-only
step; the preceding mapping verify's 76 SQL passes remain evidence for that earlier
scope. Runtime ADMIN endpoints, merged-row validation, quota/edit races and Gateway
Voucher integration remain NOT RUN; controllers/application commands are not yet
implemented. No migration, local database update, commit or push was performed.

## ADMIN Voucher application commands - 2026-11-07

Implemented AdminVoucherService and AdminVoucherRepository for list/detail,
create, partial edit and disable. ADMIN SQL uses the same booking_db Voucher row
as Booking's native reserve/consume/release commands. A transaction-scoped
SELECT WITH (UPDLOCK, ROWLOCK) obtains fresh values independently of JPA's first-
level cache. Protected edits and quota are checked under that lock; the UPDATE
also includes quota/protected-edit conditions. Only changed supplied columns are
written, and SQL increments version once. No-op and repeated disable keep version.
Duplicate code conflicts, including concurrent creates, translate to a business
conflict and roll back the whole command. No @Version or migration was added.

The first invocation stopped at compilation of the new test helper (Runnable
versus ThrowingCallable and an overloaded assertion's type inference). Those
test-code errors were corrected before the final complete run:

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\mvnw.cmd '-Dmaven.repo.local=C:/Users/Hieu/.m2/repository' -pl services/booking-service -am --batch-mode --no-transfer-progress verify
```

Final exit 0, BUILD SUCCESS. Surefire/Failsafe XML reports **185 tests PASS**,
zero failures/errors/skips: 80 unit/JSON/HTTP and 105 SQL integration tests.
The SQL total includes **29 new AdminVoucherServiceIT cases**. Coverage includes
CRUD application behavior, paging/status filters, duplicate-code rollback,
partial/no-op edits, merged type/value validation, protected edits with used or
reserved quota, exact/below quota limits, re-enable without counter reset,
stale JPA data avoidance, concurrent ADMIN same/different-field writes,
concurrent duplicate creates, reserve versus limit/protected edit/disable,
payment after disable, and re-edit after all unused reservations release while
preserving the old booking's saved amount.

Evidence: services/booking-service/target/surefire-reports/TEST-*.xml,
services/booking-service/target/failsafe-reports/TEST-*.xml and ignored local
.idea/local-backups/admin-voucher-service-verify.log. Tests used a disposable
SQL Server 2022 container; no local database configuration was used or changed.

Runtime ADMIN HTTP endpoints/status/error mapping and Gateway Voucher routing
remain NOT RUN: controllers and Gateway forwarding are the next step. The
expectedTotal/fresh-voucher-pricing changes to create booking also remain NOT RUN
and unimplemented. Existing Booking tests passing does not establish those new
pricing decisions. No commit/push was performed.

## ADMIN Voucher controllers and Gateway routing - 2026-11-07

Implemented the five user-approved ADMIN operations: GET list/detail, POST
create, PATCH edit, POST disable. Booking and Gateway enforce ADMIN independently.
Create returns 201 and Location; edit/read return 200; disable/repeat return 204
without a body. Disable rejects a request body/query parameters. Merged-row
discount validation now maps ConstraintViolationException to 400 with fieldErrors;
other Voucher validation errors use the common error shape and Voucher message.
Gateway forwards the Voucher paths to lb://booking-service; PATCH CORS support
was already present and is now covered for this route.

The initial combined run passed Gateway but found the existing broad WebMvcTest
fixture missing the new controller's AdminVoucherService dependency. A matching
MockitoBean was added without weakening its assertions. The final complete run:

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\mvnw.cmd '-Dmaven.repo.local=C:/Users/Hieu/.m2/repository' -pl infrastructure/api-gateway,services/booking-service -am --batch-mode --no-transfer-progress verify
```

Exit 0, BUILD SUCCESS. Current Surefire/Failsafe XML totals **307 PASS**,
zero failures/errors/skips: **221 Booking** (80 unit/JSON/HTTP and 141 real SQL
integration) plus **86 Gateway** tests. New coverage is **36 AdminVoucherHttpIT**
cases and **12 Gateway Voucher cases**. Booking checks exercise real controllers,
service/repository, production security and signed test JWTs against disposable
SQL Server: 401/403, create/read/filter, malformed inputs, merged-row 400 with
fieldErrors, duplicate/protected/quota 409s with rollback, no-op/version rules,
disable/repeat/enable, no-body disable, missing IDs and rejected PUT/hard DELETE.
Gateway checks exercise actual HTTP/lb routing with signed JWT/JWKS fixtures,
controlled service instances and an HTTP upstream: missing/USER/expired-token
rejection before forwarding; ADMIN methods, body/Bearer/Location/status forwarding;
and PATCH preflight without an upstream request. Existing Gateway Movie and
Booking lifecycle tests passed for their separately defined scope.

OpenAPI full validation passed again, with **five** operations counted from the
parsed document (the earlier text saying six was corrected). See
docs/postman/admin-vouchers.md for real-token checks after starting Booking with
its own database credentials and restarting Gateway. No booking-local.properties
or booking-it.properties was present during this run. Testcontainers supplied
the disposable test database; the user's local database was not changed.

Live updated Booking registration/real Identity tokens/Postman CRUD: **NOT RUN**.
The code and packaged applications are ready to start with local configuration,
but these tests do not restart the user's services or provision local accounts.
Create-booking expectedTotal/fresh-discount integration remains **NOT RUN** and
unimplemented. No commit/push was performed.

Evidence: services/booking-service/target/{surefire-reports,failsafe-reports}/TEST-*.xml,
infrastructure/api-gateway/target/surefire-reports/TEST-*.xml and ignored local
.idea/local-backups/admin-voucher-http-gateway-verify.log.

## Live ADMIN Voucher Postman checks - user-confirmed 2026-11-07

This entry supersedes the earlier NOT RUN status for the specific live requests
below. After local Booking database/configuration setup, the user executed the
guided Postman checks through Gateway and confirmed the expected results. These
are user-reported results, not an additional automated run or independently
captured response log.

| Live check | Result |
| --- | --- |
| ADMIN GET /api/v1/admin/vouchers through Gateway | PASS, user-confirmed 200 |
| POST create voucher | PASS, user-confirmed creation flow; returned voucherId 2a76f66d-3c12-4a5f-93b8-2073aceac9e0 |
| PATCH usageLimit to 120 | PASS, user-confirmed 200 and expected update |
| Repeat identical PATCH | PASS, user-confirmed no-op with unchanged version |
| POST disable and GET detail | PASS, user-confirmed 204/no body, status=false and expected version increment |
| Repeat disable and GET detail | PASS, user-confirmed 204 and unchanged version |
| List without token | PASS, user-confirmed 401 |
| List with USER token | PASS, user-confirmed 403 |
| List with ADMIN token | PASS, user-confirmed 200 |
| PATCH usageLimit=-1, then read unchanged data/version | PASS, user-confirmed 400 VALIDATION_ERROR and preserved state |
| Create duplicate HIEU10, including after disable | PASS, user-confirmed 409 VOUCHER_CODE_EXISTS |

These checks plus the separately recorded 307 automated passes provide review
evidence for the two ADMIN Voucher tasks. No additional Maven/Flutter tests were
run when recording the user's confirmation. This entry does not claim all live
quota/concurrency/payment scenarios were manually exercised: those have automated
SQL fixture coverage for the recorded implementation. Booking expectedTotal and
fresh-discount integration remain unimplemented/NOT RUN, and producer/consumer
review is still separate. No commit or push was performed by this update.

## Add Concession to Cart — 2026-11-08

Scope: Flutter customer cart under the three user-approved rules in
[cart acceptance](contracts/concession-cart-acceptance.md). Add commands read the
current quantity, cap each item at 10, and expose a read-only cart. Catalog/cart
screens bind the selection to a hold: the first hold keeps a temporary selection;
another hold clears it. Refresh removal now displays a dismissible notice.
The backend's existing quantity range is 1–99; no backend or migration changed.

Regression reproduced before the fix: two taps without a widget rebuild yielded
quantity 1 instead of 2. After the command-time read fix, the regression passed.
Two initial test-authoring issues (a tooltip finder cast and tapping before scroll
layout completed) were corrected without removing assertions.

Executed from `cinema_booking_app`:

```powershell
flutter analyze
flutter test --reporter expanded
```

- **PASS**: analyze, no issues found.
- **PASS**: full Flutter unit/widget suite, **178 tests**, exit 0. Includes **16
  new cart tests** covering rapid/repeated/mixed additions, maximum, invalid ID,
  immutable cart, temporary subtotal/price refresh, hold changes, navigation,
  non-null/unexpired hold handoff, removed items, error preservation, logout and
  account/session isolation. Existing catalog/session/movie/booking tests also
  passed for their individual fixture scopes; this is not live service acceptance.
- **PASS**: Android fixture smoke on Pixel_9a / emulator-5554, exit 0:
  `flutter test integration_test/concession_catalog_smoke_test.dart -d emulator-5554 --reporter expanded`.
  One executed scenario passed (paging/filter, two additions, cart round trip,
  no-hold continue disabled, inactive notice and retry). The separate live Gateway
  health case was **SKIPPED** because CHECK_LIVE_GATEWAY was not enabled.
  This used an on-device HTTP fixture, not real credentials or a live database.
- **PASS**: `flutter build apk --debug`, then reinstall the normal
  `lib/main.dart` APK on the emulator with `adb install -r` (Success). The headless
  emulator started for these checks was closed afterward. Runtime configuration
  remains the default; the fixture is only in integration-test code. Build log:
  `.idea/local-backups/add-cart-default-apk.log` (ignored).
- Live USER login + updated add-cart flow against real Gateway/domain services:
  **NOT RUN** in this change. Earlier live list/filter results do not establish
  the new hold/cart behavior. Use the live checklist in cart acceptance.
- expectedTotal/fresh-discount create-booking integration: **NOT RUN**, unchanged
  and unimplemented by this task. Update/remove, skip and apply-voucher tasks are
  not marked complete by these add-cart checks.

Local evidence (ignored): `.idea/local-backups/add-cart-flutter-tests.log`
(initial failure), `add-cart-flutter-tests-final.log` (178 PASS),
`add-cart-flutter-analyze.log`, and `add-cart-android-smoke.log`.
No commit/push was performed for this change.

### Live booking/payment flow — user-confirmed 2026-11-08

After preparing local demo data, the user reported reaching both successful and
failed demo-payment outcomes in the real application flow. This supersedes the
earlier NOT RUN entry for reaching those live flow outcomes only. Evidence is the
user's confirmation; no additional automated run, response capture or database
inspection was performed when recording it.

This does not independently confirm exact concession quantities/totals in the
created booking, persisted payment/ticket/seat states, voucher lifecycle, retries,
or every add-cart acceptance case. The 10-unit/rapid-tap boundary, filter/cart
navigation, no-hold and changed/expired-hold behavior, inactive-item notice and
account isolation still need specific live confirmation against the checklist.
Their recorded automated coverage remains separate. expectedTotal/fresh-discount
integration remains unimplemented by this task. No commit/push was performed.

### Update/Remove Cart Item and Skip Concessions — 2026-11-08

The user subsequently explicitly accepted Add Concession to Cart as DONE.
This records user acceptance, without inventing additional per-case live evidence.

Review reproduced two defects before fixing them: rapid decreases used a stale
rendered quantity on both screens, and rapid Skip taps pushed duplicate cart
routes. Initial focused run: 6 passed, 4 failed. Decrease now reads current state;
cart navigation is guarded until the pushed route returns.

- **PASS**: 10 focused widget tests in
  `cinema_booking_app/test/features/customer/update_remove_skip_test.dart`.
  Covers rapid decreases on both screens, mixed increase/decrease, removal to
  zero and empty-cart handoff, quantity-cap recovery, Skip with empty/populated
  carts, preservation of the actual routed hold and seat IDs, duplicate Skip,
  return/re-add, and missing/expired hold guards.
- **PASS**: `flutter test --reporter expanded`, 188 unit/widget tests, exit 0.
- **PASS**: `flutter analyze`, no issues found.
- **NOT RUN**: fresh Android/device integration for these two fixes. This review
  ran host-side Flutter widget tests; earlier Android add-cart smoke predates the
  fixes and is not recorded as their device verification.
- **NOT RUN**: live Gateway/Booking persistence and payment for update/remove or
  ticket-only Skip. Tests capture BookingSelection at the summary route; they do
  not submit an actual booking. Live credentials/data were not used in this run.

Local ignored evidence: `.idea/local-backups/update-remove-skip-before.log`,
`update-remove-skip-after.log`, `update-remove-skip-full.log`, and
`update-remove-skip-analyze.log`. No runtime defaults, backend, DB or migrations
were changed. No commit/push performed. Apply Voucher and the separate pricing
integration remain outside this review.

### User acceptance: Update/Remove and Skip — 2026-11-08

The user explicitly confirmed both tasks PASS in the live Flutter flow. The
application reached demo payment, but both simulated outcomes displayed
`Resource not found`. This records acceptance of the two cart tasks only;
demo-payment success/failure is not PASS for this run. Android verification
remains separate. No new automated execution is implied by this confirmation.
