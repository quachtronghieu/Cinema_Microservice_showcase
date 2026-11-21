# Catalog-Booking quote proposal

Status: **SUPERSEDED by the user's decision on 2026-11-07**. The 60-second concession quote proposal below is retained for history and must not be implemented as the current policy. See [Booking pricing and voucher decisions](booking-pricing-voucher-decisions.md) for create-time repricing with expectedTotal, accepted race boundaries, and outstanding voucher questions. Booking-owner review and implementation/test evidence are not implied by this user decision.

## Historical proposal (not current policy)

Original status: **PENDING REVIEW**. This was a proposal, not an accepted Booking contract.
No quote endpoint, quote acceptance, or Booking database change is implemented by the Concession CRUD task.
The user will discuss this with the Booking developer later; no message has been sent.

The single business decision to discuss is:

> Does the team accept allowing a customer to create a booking for a newly deactivated concession using a still-valid quote issued before deactivation?

Proposed policy, pending that decision:

- Catalog issues an immutable quote valid for 60 seconds, using UTC issuedAt/expiresAt.
- Catalog refuses to issue a new quote for an inactive concession.
- A quote issued while the item was active remains acceptable until its expiry, even if the item is subsequently deactivated.
- Booking stores the accepted concession ID, name, unit price and quantity as historical snapshots.
- Already created valid bookings continue payment under their own payment deadline.

Before integration implementation/acceptance, both sides must agree the decision and specify the quote ID, actor/selection binding, immutable pricing, expiry boundary, verification/replay rules and behavior under timeout. Do not use current price, an unsigned client-supplied amount or a new active lookup as a substitute for a reviewed quote protocol.

After acceptance, update both Catalog and Booking contracts and implement/run tests for quote issuance before deactivation, refusal after deactivation, quote expiry and the selected create-booking behavior. Until then these tests are **NOT RUN** because the shared contract and implementation are pending.
