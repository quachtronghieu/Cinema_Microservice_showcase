# Add Concession to Cart — Iteration 2

Scope: the customer Flutter selection cart, using the existing Catalog API and
BookingSelection handoff. No cart table, cart API or reservation of concession
stock is introduced. Booking remains authoritative for prices and availability.

## User-approved behavior (2026-11-08)

- At most 10 units per concession in the Flutter cart. Booking currently accepts
  1–99 per request item (CustomerBookingRequest); the UI limit fits that contract
  but is not a new server-enforced limit. No backend validation was changed.
- A USER may select concessions without a seat hold. This temporary cart follows
  the first hold. Moving from an existing hold to a different hold clears it;
  returning to the same hold preserves it. Returning from a held cart to a
  no-hold context also clears the previous booking's selection.
- Continuing to booking summary requires a non-null, locally unexpired hold.
  The backend must still validate hold ownership, status and expiry at creation.
- A cart is in memory for the current login session. Logout, session expiry or
  account change clears it. Persistence after app restart is outside this task.
- Adding an existing concession increments its existing line; it does not create
  duplicate lines. The badge counts total units, not distinct concessions.
- Adding reads the latest quantity at command time, including rapid taps before
  a widget rebuild. Unknown IDs cannot enter the cart. UI and ViewModel both
  enforce the maximum; the UI disables the plus button at the maximum.
- Filters and catalog/cart navigation preserve selections. A successful refresh
  removes unavailable IDs and shows a dismissible notice naming removed items.
  A failed refresh preserves existing selections and reports the loading error.
- Subtotals use the catalog's whole-VND integers and are explicitly temporary.
  A refreshed price updates the temporary subtotal; this is not a price lock.
  BookingSelection carries concession IDs and quantities, not authoritative prices.

## Verification checklist

Automated coverage belongs in
`cinema_booking_app/test/features/customer/add_concession_to_cart_test.dart`:
first/repeated/mixed additions, rapid taps, quantity cap, invalid ID, read-only cart,
subtotal, filters/navigation, temporary/first/same/different hold, expired hold,
selection handoff, refresh removal/error/price, logout/account change and stale
refresh completion. Existing catalog/session/widget tests remain regression checks.

Android fixture smoke covers real Flutter rendering and local HTTP fixture data,
including pagination, add twice, opening/returning from the cart, no-hold continue
disabled, inactive removal notice and retry. It does not prove a live Identity,
Catalog, Cinema or Booking deployment. Record executed results in
[verification](../verification.md), including NOT RUN when unavailable.

Live USER checks through Gateway before final task acceptance:

1. Add popcorn twice and a drink once: two lines, badge 3, correct temporary total.
2. Add rapidly; reach 10 on one item and verify its plus button is disabled.
3. Switch categories, open the cart and return: selections remain.
4. Without a hold, select items but verify Continue is disabled.
5. Start the first hold: temporary selections remain; use a different hold and
   verify the old selection clears. Verify an expired hold cannot continue.
6. Disable a selected fixture/demo item using the authorized ADMIN API; refresh
   the catalog and verify removal plus notice. Use agreed demo data only.
7. Logout/login as another USER and verify no cart data is inherited.

Add-cart coverage alone does not complete Update/Remove Cart Item, Skip Concessions
or Apply Voucher acceptance. expectedTotal/fresh-discount integration remains the separate
work described in [Booking pricing decisions](booking-pricing-voucher-decisions.md).

## Update/Remove and Skip review (2026-11-08)

- Decrease reads the current quantity when tapped, on both catalog and cart.
  Reaching zero removes the line; repeated removal cannot create negative counts.
  Counts and temporary subtotals reflect the remaining selection.
- Skip clears concessions and opens the existing cart review with the same hold.
  It does not release seats, renew the hold or create a booking itself.
  Continue hands off an empty concession list for a ticket-only booking.
- Both cart-opening buttons share a navigation guard against rapid double taps.
  Returning after Skip does not restore discarded items; adding again is allowed.
- Missing or expired holds still block continuation, including after Skip.

Regression tests: `test/features/customer/update_remove_skip_test.dart` (10 cases).
These use real screen/ViewModel logic with fixture repositories and a summary
capture route. They verify the BookingSelection handoff, not a live booking write.
See verification.md for executed results and outstanding live/device checks.
