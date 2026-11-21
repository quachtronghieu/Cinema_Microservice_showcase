# Catalog migration plan

The Movie section is at the end of this file.

# Concession migration plan

Scope: Catalog Concession only. Source monolith remains the current writer until a reviewed cutover. This change does not migrate live data or alter Booking.

## Target and credentials

Provision catalog_db with SQL Server 2022. Use a dedicated catalog_migrator for Flyway and catalog_app for runtime; runtime must not use sa or db_owner. Migration needs DDL rights in catalog_db; runtime needs only SELECT/INSERT/UPDATE on dbo.Concession (plus the access necessary for its transaction locks). There is no physical DELETE API. Database creation and login/password provisioning belong to environment setup; do not store credentials in Git.

The service uses Flyway, ddl-auto=validate and open-in-view=false. Migration credentials are separate from runtime credentials. Do not enable baseline-on-migrate or Hibernate update to bypass existing schema mismatches.

## New empty catalog_db

Run Flyway through application startup using the migration credential. V1 creates the original five-column Concession shape; V2 expands IDs and prices and adds type, active and version. No automatic demo seed runs in the default profile.

The 2026-11-03 whole-VND catalog-price policy changes new-write validation, not the applied V1/V2 migrations or DECIMAL(19,2) storage. Before accepting the new contract, inspect `SELECT concession_id, name, price, active FROM dbo.Concession WHERE price <> FLOOR(price)` and reconcile returned rows explicitly through versioned ADMIN PUTs. Do not round automatically, remove old IDs or change Booking snapshots. Live reconciliation is a separate manual deployment step.

## Controlled legacy import

1. Back up the source and retain row counts, ID set, sum of prices and an export checksum. Perform a rehearsal on copies. Keep the source's historical Booking_Detail data intact.
2. Preflight Concession IDs: only exact PC/DR prefixes can be mapped automatically. Check duplicate IDs, blank/invalid image references, null fields and non-positive prices. Resolve ambiguous records explicitly; do not invent a type or rewrite an ID. Existing text can be preserved even if it does not satisfy the new-write validation; document remediation before editing such rows.
3. On a new catalog_db, apply migration V1 only using Flyway target=1 (a version-specific deployment property, not a committed production default). Import just the five Concession columns from the reviewed export using parameterized/bulk loading. IDs, names, descriptions, price and image references must remain unchanged.
4. Remove the temporary Flyway target override and apply V2. All legacy items start active=true because the source catalog has no active flag; version=0; type derives from the reviewed PC/DR prefixes. V2 blocks unknown types/invalid prices. The migration is transactional, so a failure cannot leave half of the new columns applied.
5. Reconcile row count, exact IDs, names/images, prices and total price before/after. Existing IDs stay intact; only newly created concessions use UUIDs. DECIMAL(10,2) expands to DECIMAL(19,2).
6. Coordinate Booking import separately: preserve concessionId, unitPrice, quantity and backfill concessionName with evidence. The current catalog name is a reconstruction if no historical source exists; it must not be represented as a proven original sale-time name. Booking owns removing its JPA/FK dependency on Catalog during its cutover.
7. During the real cutover, stop source Concession writes, import/reconcile the final export, switch routes to one Catalog writer, then run authenticated smoke tests. Until this gate, do not route production writes to both implementations. Adapter compatibility for the legacy page/response/image routes needs separate verification.

If a migration has already been applied, never edit it. Add a new version. These new V1/V2 files apply to the newly introduced Catalog service; there was no Catalog migration history in this repository before this change.

## Recovery and evidence

On failed rehearsal, retain the source backup and fix the preflight issue before retrying against a fresh target. Flyway clean is disabled. Before new Catalog writes, a failed cutover can return to the preserved source after routing checks. After new writes, reconcile/export the Catalog delta before any route rollback. Do not drop the source or cascade-delete Booking history.

Automated disposable SQL Server tests cover a clean target, a staged V1 import followed by V2, and rejection/transactional rollback of an unknown legacy type. A real dataset export/import, backup/restore and route cutover remain NOT RUN until environment setup and reviewed source data are available.

# Movie migration

Scope: Catalog Movie only. Rating and watchlist stay in the source monolith until their own migration. This change does not migrate live data. The source monolith remains the writer until a reviewed cutover.

## Target

V3 creates the legacy nine-column Movie shape. V4 expands IDs to VARCHAR(36), adds active and version, adds `UX_Movie_active_title` (a filtered unique index on active titles), and creates Movie_Image. The runtime user also needs SELECT/INSERT on dbo.Movie_Image. No demo seed runs in the default profile.

The same staged approach as Concession applies on a new catalog_db: apply Flyway to target 3, import the nine Movie columns unchanged, then remove the override and apply V4. V4 blocks duplicate titles (ignoring case) and non-positive durations instead of merging or defaulting them, and rolls back as one unit. For a local database already at V4, use [catalog_movie_import.sql](migration/catalog_movie_import.sql) instead; a duplicate title stops that import too.

## Decisions that differ from the monolith

| Monolith behaviour | Catalog behaviour | Reason |
|---|---|---|
| DELETE removes the row and cascades to Showtime | Soft delete with `version`; rows are kept | AGENTS section 10 forbids cascading deletes of history. Cinema and Booking keep the ID as an external reference. |
| Uploads go to the classpath folder, with the client's filename in the path | Uploads go to Movie_Image with a server-generated ID, and type comes from the file signature | The classpath is read-only in a JAR and is not shared between instances. A client filename in a path is unsafe. |
| No concurrency control | Row lock plus `version` | Lost updates between admins, same pattern as Concession. |
| Duplicate title returns 400 and has a check-then-insert race | Returns 409 and the unique index closes the race | AGENTS section 6 status rules. |
| Title regex allows any whitespace | Title allows the space character only | A newline in a title is never valid. |
| Search passes `%`, `_`, `[` through as wildcards | They match literally | Predictable search results. |

Upper-case titles, the date windows, the 61-180 minute duration, the ageRating list and the YouTube trailer format are unchanged. Title and genre stay ASCII because the columns stay VARCHAR; changing that needs a separate migration.

Posters keep their `/assets/img/movies/...` URLs. The 58 legacy files are bundled into the Catalog JAR so imported rows resolve. Replaced uploads are kept deliberately, so Movie_Image grows with each replacement.

## Watchlist

V5 creates dbo.Watchlist with a foreign key to Movie only, since customer_id is an external Identity ID. A soft-deleted movie stays saved but is hidden from the list and cannot be added. Legacy watchlist rows have not been imported. The customer comes from the verified `customerId` token claim; an `X-User-Id` header is ignored, and a token without that claim gets 403.

## Showtimes

Flutter's Open Showtimes moved from `GET /api/showtimes/movie/{id}?date=` to `GET /api/v1/showtimes?movieId=&date=`, which cinema-service now filters by both values and sorts by start time.

`Showtime.movie_id` widened from 8 to 36 characters so new UUID movies fit. Since 2026-11-06 the Cinema schema comes from Flyway V1 (see "Cinema rooms, Flyway and security" below). The old test room script `docs/migration/cinema_local_seed.sql` was removed because its room R1 and seat IDs TS000001-TS000008 clashed with real rooms; [docs/dev/local-seed/06_cinema_test_room.sql](dev/local-seed/06_cinema_test_room.sql) replaces it with room 901.

## Paths moved to /api/v1

Commit 9be0bec moved these without keeping the old paths, so any client still on an old path gets 403 from the Gateway. AGENTS section 6 asks to keep adapters for existing customer APIs during the move; none exist, because Flutter moved in the same change.

| Old | New |
|---|---|
| /api/public/showtimes | /api/v1/showtimes |
| /api/admin/showtimes, /api/admin/rooms | /api/v1/admin/showtimes, /api/v1/admin/rooms |
| /api/customer/watchlist | /api/v1/customer/watchlist |
| /api/customer/seat-holds, /bookings, /tickets | /api/v1/customer/seat-holds, /bookings, /tickets |
| /api/cinema/internal/seats (service to service, not public) | /api/v1/cinema/internal/seats |

## Route mapping

| Source route | Target | State |
|---|---|---|
| GET /api/public/movies?status= | GET /api/v1/movies?status= | Flutter moved in this change. The response is now a page, and the field is `movieId` instead of `movieID`. |
| GET /api/public/movies/{id} | GET /api/v1/movies/{id} | Flutter moved in this change |
| GET /api/movies, /api/movies/{id} | GET /api/v1/admin/movies, /{id} | Target implemented; legacy admin UI is not migrated |
| POST, PUT /api/movies (multipart) | POST, PUT /api/v1/admin/movies (multipart) | PUT now also requires `version`, and the file part keeps the name `image` |
| DELETE /api/movies/{id} | DELETE /api/v1/admin/movies/{id}?version= | Soft delete |
| GET /api/movies/next-id | RETIRED | The server assigns IDs on create, per AGENTS section 17 |
| GET /api/movies/coming-soon | GET /api/v1/movies?status=coming-soon | Replaced |
| GET /api/public/movies/top | Not migrated | Needs Movie_Rating |
| GET /api/public/movies/now-showing | GET /api/v1/movies?status=now-showing | Replaced |

The Gateway still forwards `/api/movies/**` and `/api/public/**` to Catalog, which answers them with 401 because no adapter exists. No client in this repository uses them after this change.

Automated disposable SQL Server tests cover a clean target, a staged V3 import followed by V4, and rejection/rollback of duplicate titles and invalid durations. A real dataset import, backup/restore and route cutover remain NOT RUN.

# Seat map, availability and seat holds

Contract: [seat-holds.yaml](contracts/seat-holds.yaml). Booking V1 already has the hold tables, so this change adds no migration. Cinema is unchanged at the schema level. Local databases are not modified by the code; they change only when the API is called.

| Source route | Target | State |
|---|---|---|
| GET /api/showtimes/{id}/seats (seats with status) | GET /api/v1/showtimes/{id}/seats (Cinema, layout only) and GET /api/v1/showtimes/{id}/seat-availability (Booking, status and price) | Flutter moved in this change; it joins both by templateSeatId. The old combined response is not kept. |
| POST /api/customer/seat-holds | POST /api/v1/customer/seat-holds | Implemented on Booking with holds in booking_db. Request and response fields are unchanged. |
| DELETE /api/customer/seat-holds/{holdId} | DELETE /api/v1/customer/seat-holds/{holdId} | Implemented on Booking. Repeating a release returns 204; a consumed hold returns 409. |

Differences from the monolith: another customer's hold returns 403 HOLD_NOT_OWNED instead of 404 (AGENTS section 6), and a seat in a hold that has expired stays HELD until the expiry job runs, at most a few seconds. The monolith's startup cleanup of "orphan" PENDING seats is not carried over, because holds no longer live in memory (AGENTS section 20).

`Cinema.Showtime_Seat` and the internal lock/release controller remain only because Open Sales still creates those rows. No customer route reads them now. Removing them is scope item R-02/Tho's Open Sales work, through a new migration.

# Cinema rooms, Flyway and security

Contract: [rooms.yaml](contracts/rooms.yaml). cinema-service now owns its schema through Flyway: V1 creates Template, Template_Seat, Room (INT IDENTITY, many rooms per template), Showtime and Showtime_Seat; V2 creates Room_Status_Audit. Hibernate runs with `ddl-auto: validate`, and the old `update` mode and default `sa` credentials are gone. Runtime and migration use separate logins, as in Booking and Catalog.

An existing cinema_db that Hibernate created cannot take V1 (its tables exist and room_id is text), and `baseline-on-migrate` stays off (AGENTS section 10). Local databases move by renaming the old one, letting Flyway build a new empty cinema_db, then copying data with [05_cinema_copy_to_flyway_db.sql](dev/local-seed/05_cinema_copy_to_flyway_db.sql). The script keeps every ID and copies only rooms with numeric IDs; it lists rows it skips and refuses to run on a database that already has data. It was rehearsed on a disposable SQL Server with an old-shaped copy.

| Route | State |
|---|---|
| GET /api/v1/admin/rooms | Now a page `{items,page,size,totalElements,totalPages}` of DTOs; it used to return a bare array of entities |
| GET /api/v1/admin/rooms/{id} | DTO with capacity; non-numeric IDs such as R1 return 404 |
| GET /api/v1/admin/rooms/{id}/template | New |
| PATCH /api/v1/admin/rooms/{id}/status | New; rules in docs/ngan-task-scope.md |
| Create, update, delete room or template | Not provided, by scope decision |
| /api/v1/cinema/internal/seats/** | Denied by cinema-service until service tokens exist (AGENTS section 7); no caller uses it |

cinema-service now checks tokens itself: GET /api/v1/showtimes/** stays public, /api/v1/admin/** needs ADMIN, everything else is denied. Showtime create, update and Open Sales lock the room row and return 409 ROOM_INACTIVE for an INACTIVE room. Errors use the common body `{code,message,traceId,fieldErrors}`.
