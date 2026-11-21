# ADMIN Voucher checks through Gateway

All requests below target Gateway at `http://localhost:8080`. They require a fresh
Identity accessToken with role ADMIN unless the check explicitly calls for USER
or no token. Do not commit real tokens, passwords, or generated local environments.

## Start the updated applications

1. Run Discovery and Identity using the existing local setup.
2. Run `BookingServiceApplication` with its own booking_db configuration. The
   checked-in `booking-local.properties.example` documents the required separate
   runtime/migration credentials. If needed, copy it to the ignored
   `booking-local.properties` and fill values for your own provisioned database.
   Do not reuse Catalog's database or credentials. Do not manually recreate
   Voucher: the existing Booking V1 Flyway migration owns the schema.
3. To load that root file from IntelliJ, use this Program argument, adjusting the
   absolute path if the checkout moves:

   ```text
   --spring.config.additional-location=file:D:/Project_MSS301/CinemaManagementMicroService/booking-local.properties
   ```

4. Start/restart Gateway so the new Voucher route is loaded. Verify
   BOOKING-SERVICE is UP in Eureka. The current checked-in Booking application
   uses port 8083, but Postman always calls Gateway 8080.
5. Log in through Identity's existing Gateway endpoint and choose Postman
   Authorization -> Bearer Token. Paste only the accessToken value. Repeat login
   if expired or Identity was restarted with a new signing key.

These local startup and live-token steps are manual checks, not results inferred
from the automated HTTP/SQL/Gateway fixtures.

## Request sequence

Set Content-Type: application/json for create/PATCH bodies.

1. **Read**: `GET /api/v1/admin/vouchers?page=0&size=20` -> 200, paged DTO.
   No token -> 401; a USER token -> 403. Errors contain code, message, traceId,
   fieldErrors. If no Booking instance is available, valid ADMIN traffic returns
   a dependency error rather than an empty successful list.
2. **Create**: `POST /api/v1/admin/vouchers` with the following body -> 201 and
   Location. Change the example code if already used; duplicate code returns 409.

   ```json
   {
     "code": "HIEU10",
     "voucherName": "Hieu ten percent discount",
     "discountType": "PERCENTAGE",
     "discountValue": 10,
     "usageLimit": 100
   }
   ```

   Save voucherId from the response. Expected defaults: status=true,
   usedCount=0, reservedCount=0, remainingCount=100, version=0.
3. **Detail**: `GET /api/v1/admin/vouchers/{voucherId}` -> 200.
4. **Edit**: `PATCH /api/v1/admin/vouchers/{voucherId}` with
   `{"usageLimit":120}` -> 200; a changed command increments version once.
   Sending the same value again is a no-op with unchanged version. Do not send
   version or counters in the request. Omitted fields remain unchanged;
   explicit null, unknown fields and an empty object return 400.
5. **Disable**: `POST /api/v1/admin/vouchers/{voucherId}/disable` with no body
   and no query parameters -> 204 and no body. Repeating returns 204 without
   another version increment. Read the detail to see status=false.
6. **Optional status edit**: `PATCH /api/v1/admin/vouchers/{voucherId}` with
   `{"status":true}` -> 200. Counters are preserved; enabling does not create
   quota when all uses are already used/reserved.

Validation examples: a percentage above 100 or negative usageLimit returns 400.
Code/type/value changes with usedCount>0 or reservedCount>0 return 409
VOUCHER_EDIT_RESTRICTED. A limit below usedCount+reservedCount returns 409
VOUCHER_LIMIT_CONFLICT. Do not change counters directly in a shared/live DB to
simulate these cases: automated SQL fixtures exercise them; live lifecycle
checks should create bookings through the authorized Booking APIs.

ADMIN Voucher APIs do not implement the separate expectedTotal/fresh-discount
booking policy. See [decisions](../contracts/booking-pricing-voucher-decisions.md)
and [acceptance evidence](../verification.md) before claiming integration completion.
