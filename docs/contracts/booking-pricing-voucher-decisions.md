# Booking pricing and voucher decisions

Date: 2026-11-07.

Status: user decisions recorded. ADMIN Voucher CRUD is implemented and locally tested as described in [verification](../verification.md). Create-booking expectedTotal/fresh-discount changes remain unimplemented; joint producer/consumer review and live integration acceptance are still pending. This document records decisions rather than asserting all related features have shipped.

## Create-time pricing: accepted by the user

- Drop the proposed 60-second concession quote guarantee. Preview is informational; create booking checks Catalog again.
- Create requires expectedTotal; preview does not. Backend computes the authoritative total using BigDecimal and the existing scale-2/HALF_UP money rules. expectedTotal is a comparison value, never the source of the price.
- Compare numeric values, so 150000 and 150000.00 are equal. A different final total, whether higher or lower, returns HTTP 409 and requires the customer to review and confirm again. The exact error code and refreshed-price response still need the API contract.
- Unavailable concessions, invalid/disabled vouchers and exhausted quota retain distinct business failure handling; do not silently remove a selection or voucher to make a booking succeed. Voucher expiry is outside the current scope.
- Any failure after reserving voucher quota rolls back the transaction, including quota, reservation, booking, hold consumption and idempotency success records.
- Include normalized expectedTotal in the idempotency request hash. Retry the identical request with the same key. Customer confirmation of a changed total uses a new key. Define compatibility with existing stored request hashes before rollout; do not silently invalidate existing replay records.
- expectedTotal protects the final total only. It does not detect compensating changes in individual prices when the final total is unchanged.

## Catalog race boundary: accepted by the user

Read Catalog during the create request, before acquiring Booking SQL locks. Booking may commit using that successful read even if the item is subsequently deactivated or repriced before the Booking transaction commits. No active-at-commit or cross-database atomicity guarantee is made. If Catalog reports the item unavailable during the read, reject the request. Do not use an earlier preview or a fallback price in place of this read.

Persist concession ID, name, unit price and quantity as booking snapshots. Already-created valid bookings pay from their snapshots within their own payment deadline. Neither preview nor expectedTotal extends a seat hold or the payment window.

## Voucher quota and disable: accepted by the user

- Preview does not reserve a voucher use. Create must acquire a use atomically; payment consumes it; cancellation/expiry releases an unpaid booking's reserved use once.
- Disabling blocks new reservations. A booking that already reserved a use may still pay within its payment window using its saved discount.
- Change usageLimit with a conditional UPDATE that enforces new usageLimit >= used_count + reserved_count at the database operation. Preserve the existing database quota constraint.
- Malformed/negative input is HTTP 400, an absent voucher is HTTP 404, and a quota conflict is HTTP 409. Do not expose a constraint exception as HTTP 500.
- ADMIN and Booking commands must coordinate on the same voucher row so reserve, disable, limit updates and discount edits obey their predicates atomically.

## ADMIN commands and version: accepted by the user

Valid concurrent ADMIN edits to the same field use last-writer-wins in database serialization order, not browser click or request arrival order. Every command still enforces quota and editability conditions against the current row. Update only the command's intended fields; do not save a stale full entity over quota counters or unrelated fields.

> Cột version chỉ để đánh dấu số lần thay đổi, không dùng @Version vì booking làm nó thay đổi liên tục.

Keep the existing version column and Booking SQL increments. Successful state-changing ADMIN commands also increment version atomically in SQL (`version = version + 1`). Do not add JPA @Version. The column is not an optimistic-concurrency precondition for ADMIN requests. Define no-op/repeated-command behavior explicitly in the API contract; this decision does not require a version increment for an operation that changes nothing.

## Voucher edit restrictions: accepted by the user

The user clarified these rules on 2026-11-07:

- Code, discount type and discount value can change only when used_count = 0 AND reserved_count = 0. Enforce this predicate atomically with the edit, serialized against Booking reservation updates; a prior unlocked read is insufficient.
- If all reservations are released and no use was consumed, these fields become editable again. Historical reservation existence alone does not permanently lock the voucher. Existing booking snapshots must remain unchanged.
- If used_count > 0 or reserved_count > 0, changes to those three fields are forbidden. A preview alone does not hold quota and therefore does not establish this restriction.
- Valid usageLimit changes and status changes remain allowed while reservations exist, under the quota and disable rules above. This restriction does not block description or expiry edits if those fields are later introduced and their contracts agreed.
- The user requested checking existing schema before adding description/expiry. Repository inspection found voucher_name in V1, but no Voucher description/expiry fields in any current Booking migration or in the Voucher entity. voucher_name is a name, not an existing description field; it is also not yet mapped in the entity. This is source-schema evidence, not an inspection of the user's live SQL Server database. No new fields or migrations are authorized merely by this clarification.

## Current Voucher field scope: accepted by the user

On 2026-11-07 the user accepted implementing Voucher administration with the existing schema fields only. Do not add a description or expiry field/API rule in this task. Use voucher_name as the existing voucher name and complete its entity/DTO mapping as needed; do not repurpose it as a description. Preserve existing quota counters and version semantics. No migration is needed merely to introduce description/expiry because those additions are deferred.

This scope decision does not remove seat-hold expiry or the booking payment deadline and release behavior. Adding Voucher description/expiry later requires a separately agreed contract and a new migration, never an edit to an applied migration.

## Implementation requirements and remaining contract work

- The ADMIN command shapes and validation are in [vouchers.yaml](vouchers.yaml), with the implementation order and runtime acceptance matrix in [vouchers-acceptance.md](vouchers-acceptance.md). The user approved these API details on 2026-11-07. Booking-owner review and evidence of working endpoints are still separate requirements.
- Proposed safe transaction sequence: fetch Catalog outside the transaction; validate/lock the hold; reserve voucher quota conditionally; obtain fresh discount data under the voucher row's write protection; compute and compare the total; save booking/snapshots/reservation and consume the hold; commit. All failures roll back. A previously cached JPA entity must not substitute for a fresh discount read. Agree the lock order with existing payment/cancel paths.
- Define command paths/payloads/status codes, allowed fields, repeated-command behavior, and validation before implementation. Coordinate Booking and Flutter expectedTotal changes and idempotency compatibility.
- Required tests, all NOT RUN for these new decisions: changed/unchanged totals; numeric-scale equivalence; rollback after quota reservation; idempotent replay and changed expectedTotal; disable versus reserve; limit decrease versus reserve; concurrent ADMIN edits; fresh discount versus ADMIN edit; re-edit eligibility after all reservations are released with no consumed uses; and preserved historical booking/payment amounts. Voucher expiry tests are outside this task's scope.

No message has been sent to another team member. User approval here is not evidence that the Booking owner reviewed the contract or that integration acceptance passed.
