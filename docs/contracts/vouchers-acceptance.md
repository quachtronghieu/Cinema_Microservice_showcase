# ADMIN Voucher implementation guide

Scope: Hiếu's two Iteration 1 Voucher tasks, in booking-service / booking_db.
The user-approved [OpenAPI](vouchers.yaml) documents the concrete API shapes. User-agreed
business rules remain in [the decisions](booking-pricing-voucher-decisions.md).
Contract validity does not establish endpoint implementation or team review.

The user approved the draft on 2026-11-07. This approves its documented API
details; it does not imply Booking-owner review or runtime acceptance.

Technical validation on 2026-11-07: PASS with openapi-spec-validator 0.7.2
for the complete OpenAPI 3.0.3 document, plus 9 request-schema checks using
openapi-schema-validator (valid percentage/fixed create, percentage over 100,
negative limit, forbidden version input, partial edit, empty edit, null status,
unknown description). Five operations are described. These are schema checks,
not runtime API or database tests.

## Implementation order

1. Review the draft command shapes, normalization, error names and no-op behavior.
2. Complete the existing Voucher mapping for voucher_name and read-only version
   without @Version. Preserve existing constructors/callers or adapt them together.
   Do not introduce a second entity for the same table. Keep quota mutation in SQL.
3. Add ADMIN request/response/page DTOs. PATCH must distinguish absent fields from
   explicitly null fields, reject unknown and server-owned fields, and validate
   the resulting discount type/value pair. Never treat null as an omitted field.
4. Add application commands using one consistent voucher row locking strategy.
   Check protected edits against current counts and quota changes under that lock.
   Persist only supplied ADMIN fields; use version=version+1 on an actual change.
   Preserve reserve/payment/release SQL increments and constraints.
5. Add thin ADMIN controllers and global exception mappings. Booking already
   protects /api/v1/admin/**; separately check Gateway forwarding/security and
   configure the voucher routes there. No Flutter ADMIN screen is required.
6. Coordinate create-booking pricing with its owner: read Catalog outside SQL
   locks; reserve then freshly read voucher discount inside the transaction.
   The current PricingService reads the voucher before reserving it. ADMIN field
   restrictions alone do not fix stale pricing if a reserve/release cycle occurs
   before create reserves its use. Keep this as an integration dependency.
7. Test locally, then use real ADMIN/USER tokens through Gateway. Record actual
   PASS/FAIL/NOT RUN, never infer a pass from test code or OpenAPI validation.

No migration is required solely for mapping existing voucher_name/version.
Description/expiry are deferred. Do not edit applied V1 or reset counters/history.
expectedTotal and its Flutter/idempotency compatibility changes are a coordinated
Booking task; this guide does not mark them implemented by ADMIN CRUD.

## Acceptance matrix

Current status: ADMIN controllers and Gateway routing are implemented. Automated
Booking HTTP/SQL and Gateway HTTP-fixture checks passed for the executed scope
recorded below. The user subsequently confirmed live Gateway/Identity-token
Postman CRUD, no-op/disable repetition, 401/403, negative-limit validation and
duplicate-code checks as **PASS** on 2026-11-07; see the dated scope in
[verification](../verification.md). This does not imply all live lifecycle and
concurrency scenarios were manually tested or a separate Eureka dashboard audit.
The expectedTotal/fresh-discount creation protocol remains unimplemented and
**NOT RUN**, including the pricing-race row in the matrix.
Entity mapping step completed on 2026-11-07: full Booking verify PASS, 105 tests
(29 unit/HTTP + 76 SQL integration, zero failures/errors/skips), including three new
VoucherMappingIT cases. This does not establish any ADMIN endpoint behavior below.
DTO step completed on the same date: 80 unit/JSON/HTTP tests PASS, including 51 new
VoucherDtoTest cases. This establishes request parsing/Bean Validation and response
serialization, not HTTP status/error mapping or validation against a locked DB row.
Application command step completed on the same date: full Booking verify **185
PASS** (80 unit/JSON/HTTP + 105 SQL integration, zero failures/errors/skips), including
**29 new AdminVoucherServiceIT cases**. These establish list/create/edit/disable at
the application/SQL layer, protected-edit and quota predicates, no-op/version
behavior, same/different-field ADMIN concurrency, concurrent duplicate create,
reserve versus limit/edit/disable, payment after disable and re-edit after release.
They do not establish ADMIN endpoint authorization, HTTP error serialization,
Gateway routing or the future expectedTotal/fresh-discount creation protocol.

The subsequent controller/Gateway step completed on 2026-11-07: combined verify
**307 PASS**, zero failures/errors/skips. Booking contributes 221 cases (80
unit/JSON/HTTP + 141 SQL integration), including 36 new AdminVoucherHttpIT cases.
Gateway contributes 86 cases, including 12 new Voucher route/auth/CORS cases.
These later results supersede the prior HTTP NOT RUN status: signed test JWTs
exercise actual Booking controller/security/validation/error mapping with real
SQL Server; Gateway tests use the production lb:// route, signed JWT/JWKS fixtures
and controlled HTTP upstreams. They do not prove live service registration or
real Identity credentials. Use the [Postman runbook](../postman/admin-vouchers.md)
after starting/restarting the changed applications.

| Check | Expected behavior |
| --- | --- |
| Missing/expired token; USER token | 401; 403, at Gateway and Booking |
| ADMIN list/detail | 200; includes disabled/exhausted; missing ID 404 |
| Paging/filter | page 0, default 20, max 100, stable ID ordering; invalid 400 |
| Create | 201 + Location; UUID, enabled, zero counters and version |
| Create validation | blank/missing/null/unknown fields, invalid discount/type/limit -> 400 |
| Duplicate code | 409 even if case differs or the previous voucher is disabled |
| Partial edit | Omitted fields preserved, explicit null/empty body rejected |
| Protected edit with reserved/used count | 409; no partial writes or version increment |
| Reservations all released, no consumed use | Code/type/value editable again; old booking snapshots unchanged |
| Name/limit/status with existing use | Allowed subject to current quota; never overwrite counters |
| Limit equals used+reserved | Accepted; zero additional uses |
| Limit below used+reserved | 409; old state retained |
| Unchanged edit / repeated disable | 200 unchanged / 204; unchanged version |
| Actual ADMIN change | One SQL version increment per command; no @Version |
| Two ADMIN commands | Valid same-field last-writer-wins; different supplied fields preserved |
| Reserve versus limit decrease | Serialized conditional writes; quota invariant retained |
| Reserve versus disable/protected edit | Only a valid serialized result; no stale predicate checks |
| Booking lifecycle | Reserved booking pays after disable; cancel/expiry releases once |
| Pricing race integration | Fresh discount after reserve; full rollback on failed expectedTotal check |

Use disposable SQL Server fixtures for concurrency, constraints and native SQL.
Unit/mock tests alone do not establish database race behavior.
