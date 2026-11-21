# Bookings & Payment (booking-service) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây `services/booking-service` để Flutter làm được 9 chức năng của feature "Bookings & Payment": xem tóm tắt đơn, tạo đơn, chống gửi trùng, xem danh sách/chi tiết đơn, hủy đơn, thanh toán demo, xem vé do server phát, ADMIN check-in.

**Architecture:** Service Spring Boot MVC + JPA riêng (`booking-service`, cổng 8083 theo cấu hình gateway/Prometheus của nhóm, DB `booking_db`), đăng ký Eureka, đi qua Gateway bằng `lb://booking-service`. Giữ nguyên các đường dẫn `/api/customer/...` mà app Flutter đang gọi (adapter tương thích, AGENTS §6). Mọi bất biến ghế/tiền/vé nằm trong transaction SQL Server cục bộ của booking; không giữ khóa SQL khi gọi HTTP.

**Tech Stack:** Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0 (Eureka client, OpenFeign, Resilience4j), Spring Security OAuth2 Resource Server (RS256/JWKS), Spring Data JPA, Flyway, SQL Server 2022, Testcontainers (mẫu: `services/catalog-service`).

**Spec:** `AGENTS.md` §3, §4, §5 (5.1–5.4), §6, §7, §18.1, §19.3, §23, §24, §25, §27; `docs/contracts/catalog-booking-quote-proposal.md`; code legacy trong `Cinema_Management/.../services/Customer*Service.java` để giữ hành vi.

## Phân công liên quan (backlog chính thức `MSS301_Backlog`, sheet "Backlog")

| Phần booking cần | Người phụ trách | Thay thế tạm trong lúc chờ |
|---|---|---|
| 9 chức năng Bookings & Payment + schema booking | Lê Trần Tường Vy | — |
| Hold / Release ghế, Load Real Seat Availability, Show Hold Expiry & Conflict | Nguyễn Đào Thu Ngân | `docs/dev/create_test_hold.sql` + Flutter `/booking-summary-debug` |
| Open / Close Showtime Sales (đồng bộ inventory ghế sang booking) | Phan Đức Thọ | `docs/migration/booking_sales_snapshot_import.sql` |
| Concession & Cart, quote, Apply Voucher, Manage Vouchers | Quách Trọng Hiếu | Gọi `GET /api/v1/concessions/{id}` của catalog; voucher đọc từ bảng `Voucher` |

Sheet "Backlog tiếng Việt" giao giữ ghế và thu tiền tại quầy cho Vy; plan này theo sheet "Backlog" và AGENTS 1.4 (không có thu tiền tại quầy).

## Quyết định cần chốt trước khi code

| # | Câu hỏi | Mặc định đề xuất nếu không có ý kiến khác |
|---|---|---|
| D1 | Có bộ tài liệu 1.3 (`reference/database/booking/V1__initial_schema.sql`, `docs/contracts/core.yaml`) không? | Nếu không có: tự viết V1 theo AGENTS §19.3/§24, chỉ gồm bảng cần cho 9 task. |
| D2 | **Giữ ghế (seat hold)** do Ngân phụ trách nhưng chưa code; Create Booking phụ thuộc vào nó. | Điểm nối là dữ liệu: Task 0 tạo sẵn bảng `Showtime_Seat/SeatHold/ActiveHold/SeatHold_Item` trong `booking_db`; booking chỉ đọc/tiêu thụ hold. Trong lúc chờ: hold tạo bằng `docs/dev/create_test_hold.sql` (chỉ máy dev) + màn Flutter `/booking-summary-debug`; test dùng fixture. Khi Ngân xong: API `/api/customer/seat-holds` ghi vào đúng các bảng đó (cột mới = migration V2+), màn chọn ghế chuyển sang `/booking-summary`; code booking không đổi. |
| D3 | Món bắp nước bị admin tắt giữa chừng (quy tắc bạn đã nêu): giữ món trong bao lâu? | Theo đề xuất của Hiếu: báo giá (quote) sống 60 giây; đơn đã tạo thì giữ món tới hết hạn thanh toán 5 phút. |
| D4 | Lấy suất chiếu/ghế từ đâu khi **cinema-service chưa có** (Open Sales của Thọ)? | Script ETL dev nạp `Sales_Snapshot` + `Showtime_Seat` từ `Cinema_Management1`. Khi Thọ xong thì dùng API `init-sales` nội bộ thay script. |
| D5 | Ranh giới voucher giữa Hiếu và Vy (bảng `Voucher` thuộc `booking_db`). | Hiếu: CRUD voucher, giới hạn lượt, API kiểm tra mã. Vy: giữ lượt khi tạo đơn, hoàn đúng một lần khi hủy/hết hạn, chốt lượt khi thanh toán (`reserved_count`/`used_count` + `Voucher_Reservation`). Dùng chung schema V1. |

## Global Constraints

- Cấu trúc package theo lớp (AGENTS §9): `vn.edu.fpt.cinema.booking.{controller, service, entity, repository, dto, client, config, security, exception}`; test đặt cùng package với lớp được test, dữ liệu dùng chung ở `support`.
- Java 21; Spring Boot 3.5.4; Spring Cloud BOM 2025.0.0; dùng parent `pom.xml` gốc, không tự chọn version riêng.
- `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`, Flyway `baseline-on-migrate=false`, `clean-disabled=true`; thời gian UTC (`DATETIME2(3)`), API ISO-8601 có `Z`.
- Tiền: `BigDecimal` / `DECIMAL(19,2)`, VND, làm tròn `HALF_UP` scale 2; không dùng `double`; không bao giờ dùng giá 0 làm fallback.
- ID mới: UUID lowercase `VARCHAR(36)`; giữ ID legacy; cấm `max(id)+1`.
- Lỗi: `{code, message, traceId, fieldErrors}`; HTTP 200/201/204/400/401/403/404/409/503 theo AGENTS §6.
- JWT: RS256, `iss=cinema-identity`, `aud=cinema-api`, roles chỉ `USER`/`ADMIN`; `customerId` lấy từ claim `customerId` của token, không từ body.
- Hold: 1–8 ghế, 5 phút. Cửa sổ thanh toán: 5 phút kể từ khi tạo booking (tách biệt hold).
- Trạng thái: inventory `AVAILABLE→HELD→SOLD`; booking `PENDING_PAYMENT→PAID|CANCELLED|EXPIRED`; payment `PENDING→SUCCEEDED|FAILED`. Không lùi trạng thái cuối.
- Idempotency: key ASCII 1–128 ký tự, không khoảng trắng, unique `(actorId, operation, key)` collation BIN2, giữ ≥ 24 giờ, ghi cùng transaction.
- Gọi service khác: Feign theo tên Eureka, timeout connect 2s / read 3s, Resilience4j circuit breaker; không retry lệnh ghi.
- Thanh toán demo chỉ bật khi `booking.demo-payment.enabled=true` (profile `demo`).

## Review Focus

1. **Hai thao tác cùng lúc trên một đơn** (thanh toán SUCCESS và hủy, hoặc thanh toán đúng lúc job hết hạn chạy) → đúng một kết quả cuối; test ở Task 6 và 7.
2. **Voucher còn đúng 1 lượt, hai khách dùng cùng lúc** → chỉ một đơn được giảm giá; test ở Task 2.
3. **Catalog sập khi xem/tạo đơn có bắp nước** → 503 `DEPENDENCY_UNAVAILABLE`, không tạo đơn với giá 0; test ở Task 1 và 2.
4. **Khách đoán ID đơn/vé của người khác** → 403, không lộ dữ liệu; test ở Task 5 và 8.
5. **Bấm "Tạo đơn" hai lần / app retry sau timeout** → đúng một booking; test ở Task 3.

## Thứ tự thực hiện

| Thứ tự | Task | Chức năng trong backlog | Vì sao đứng ở đây |
|---|---|---|---|
| 0 | Nền booking-service | (hạ tầng chung) | Mọi chức năng cần service, DB, bảo mật, route gateway |
| 1 | Task 1 | Review Booking Summary | Chỉ đọc + tính tiền; dùng lại cho Create |
| 2 | Task 2 | Create Booking | Lõi nghiệp vụ, dùng logic tính tiền Task 1 |
| 3 | Task 3 | Prevent Duplicate Submission | Bọc ngay Create trước khi Flutter gọi thật |
| 4 | Task 4 | View Booked Detail | Cần để kiểm tra kết quả Create/Payment |
| 5 | Task 5 | View My Booked | Danh sách dựa trên cùng DTO của Task 4 |
| 6 | Task 6 | Cancel Eligible Booking (+ job hết hạn) | Tạo hàm nhả ghế/voucher dùng lại cho Payment FAILED |
| 7 | Task 7 | Process Demo Payment | Cần nhả ghế (Task 6); phát vé |
| 8 | Task 8 | Show Server Issued Ticket | Vé chỉ có sau Payment |
| 9 | Task 9 | Check-in Ticket For Admin | Cần vé đã phát |

---

### Task 0: Nền booking-service

**Files:**
- Create: `services/booking-service/pom.xml` (module con của `pom.xml` gốc; thêm vào `<modules>`)
- Create: `services/booking-service/src/main/java/vn/edu/fpt/cinema/booking/BookingServiceApplication.java`
- Create: `.../booking/config/SecurityConfiguration.java`, `.../config/TraceIdFilter.java` (theo mẫu catalog, nhận `X-Trace-Id` từ gateway nếu có)
- Create: `.../booking/exception/ApiError.java`, `ApiExceptionHandler.java`, `BookingException.java`
- Create: `src/main/resources/application.yaml` (port 8083, `spring.application.name=booking-service`, datasource/flyway qua env như catalog)
- Create: `src/main/resources/db/migration/V1__booking_schema.sql`
- Create: `docs/migration/booking_sales_snapshot_import.sql` (ETL dev từ `Cinema_Management1`, chạy lại an toàn)
- Create: `docs/dev/create_test_hold.sql` (chỉ dev: tham số `@customerId`, `@showtimeId`, `@seatCount` 1–8, `@minutes`; chọn ghế `AVAILABLE`, tạo `SeatHold` + `SeatHold_Item` + `ActiveHold`, chuyển ghế sang `HELD`; in `holdId` và `showtimeSeatIds`. Thay thế bằng API của Ngân khi có)
- Create: `booking-local.properties.example` (gốc repo), thêm `/booking-local.properties` vào `.gitignore`
- Modify: `infrastructure/api-gateway/src/main/resources/application.yml` (route booking → `lb://booking-service`)
- Modify: `infrastructure/api-gateway/.../SecurityConfiguration.java` (`/api/v1/admin/tickets/**` ADMIN)
- Test: `src/test/java/.../booking/BookingSchemaIT.java`, `.../SecurityWebTest.java`

**Interfaces — Produces:**
- Bảng V1 (tên theo AGENTS §24): `Sales_Snapshot(showtime_id PK, snapshot_version, sales_status OPEN/CLOSED, movie_id, movie_title, room_id INT, room_name, show_date, start_time_utc, end_time_utc)`, `Showtime_Seat(showtime_seat_id PK, showtime_id FK, template_seat_id, row_label, seat_number, seat_type, unit_price DECIMAL(19,2), status, hold_id NULL, booking_id NULL, version BIGINT, UNIQUE(showtime_id, template_seat_id))`, `SeatHold`, `ActiveHold(PK customer_id+showtime_id)`, `SeatHold_Item`, `Voucher(+reserved_count)`, `Voucher_Reservation(booking_id UNIQUE)`, `Booking(+channel, expires_at, voucher_id, total_amount, status)`, `Booking_Detail(item_type, showtime_seat_id NULL, concession_id NULL, name_snapshot, quantity, unit_price, total_price computed)`, `Payment(booking_id UNIQUE)`, `Ticket(booking_id UNIQUE, checked_in, checked_in_time, checked_in_staff_id)`, `IdempotencyRecord(UNIQUE actor_id, operation, idem_key COLLATE Latin1_General_100_BIN2)`.
- Gateway: `Path=/api/customer/seat-holds/**, /api/customer/bookings/**, /api/customer/tickets/**, /api/v1/admin/tickets/**` → `lb://booking-service`. Bỏ `/api/customer/**` chung để watchlist không bị gửi nhầm sang booking.
- Lớp `CurrentCustomer` đọc `customerId` từ `Jwt`; thiếu claim → 403 `CUSTOMER_ACCOUNT_REQUIRED`.

- [ ] **Step 1: Test fail:** `BookingSchemaIT.flywayAppliesV1_andHibernateValidates` (Testcontainers SQL Server, context khởi động OK); `SecurityWebTest.noToken_401_withErrorJson`, `adminRouteWithUserToken_403`.
- [ ] **Step 2:** Chạy `.\mvnw.cmd -pl services/booking-service -am verify` → FAIL (chưa có module).
- [ ] **Step 3:** Tạo module, config, V1, security, error handler theo mẫu catalog.
- [ ] **Step 4:** `.\mvnw.cmd -pl services/booking-service -am verify` → PASS. Chạy app thật: Eureka thấy `BOOKING-SERVICE` UP; `GET http://localhost:8080/api/customer/tickets` không token → 401.
- [ ] **Step 5:** Chạy `docs/migration/booking_sales_snapshot_import.sql`; kiểm tra số dòng `Showtime_Seat` = số dòng legacy `Showtime_Seat`.
- [ ] **Step 6:** Commit `feat(booking): bootstrap booking-service with schema, security and gateway route`.

### Task 1: Review Booking Summary

**Files:**
- Create: `.../booking/controller/QuoteController.java` (`POST /api/customer/bookings/preview`)
- Create: `.../booking/service/PricingService.java`, `.../booking/client/CatalogClient.java` (Feign `catalog-service`)
- Create: `.../booking/dto/CustomerBookingRequest.java`, `BookingQuoteResponse.java`
- Test: `.../service/PricingServiceTest.java`, `.../controller/QuoteWebTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/concessions/{id}` của catalog (trả `concessionId, name, price, active`). `shortcut: chuyển tiếp token của khách, đổi sang quote nội bộ khi D3 được duyệt.`
- Produces: `PricingService.quote(String customerId, CustomerBookingRequest req): BookingQuote` (dùng lại ở Task 2).
- Request: `{holdId, showtimeId, showtimeSeatIds[1..8], concessions[{concessionId, quantity≥1}], voucherCode?}`. Giữ đúng tên field của Flutter (AGENTS §18.1).
- Response: `{ticketItems[], concessionItems[], ticketSubtotal, concessionSubtotal, subtotal, voucher{voucherId, code, discountType, discountValue}, discountAmount, total}`; mỗi item `{itemType, itemId, name, quantity, unitPrice, lineTotal}`.
- Không thay đổi hold, không giữ quota voucher.

- [ ] **Step 1: Test fail** (`PricingServiceTest`):
  - `twoStandardOneVipTwoCoca_totals350000`: ghế 80000×2 + 100000 + Coca 45000×2 → `subtotal == 350000.00`.
  - `percentVoucher10_discount35000_total315000`.
  - `fixedVoucherLargerThanSubtotal_totalZero_notNegative`.
  - `sameConcessionTwice_mergedIntoOneLine_quantitySummed`.
  - `QuoteWebTest.holdOfAnotherCustomer_403`, `expiredHold_409_HOLD_EXPIRED`, `inactiveConcession_409_CONCESSION_UNAVAILABLE`, `catalogDown_503_DEPENDENCY_UNAVAILABLE`.
- [ ] **Step 2:** Chạy test → FAIL.
- [ ] **Step 3:** Cài đặt. Đọc hold + ghế từ DB; giá ghế lấy `Showtime_Seat.unit_price`; giá món lấy từ catalog **trước**, không mở transaction ghi.
- [ ] **Step 4:** Test → PASS; Postman: preview với hold có sẵn → 200 đúng tổng tiền.
- [ ] **Step 5:** Commit `feat(booking): preview booking quote with server-side pricing`.

### Task 2: Create Booking

**Files:**
- Create: `.../booking/controller/CustomerBookingController.java` (`POST /api/customer/bookings`)
- Create: `.../booking/service/BookingService.java`, entity + repository cho `Booking`, `BookingDetail`, `ShowtimeSeat`, `SeatHold`, `Voucher`, `VoucherReservation`
- Test: `.../service/CreateBookingIT.java` (Testcontainers SQL Server, có test đồng thời)

**Interfaces:**
- Consumes: `PricingService.quote(...)` (Task 1).
- Produces: `BookingService.create(String customerId, CustomerBookingRequest req): BookingCreated` với `BookingCreated{bookingId, status="PENDING_PAYMENT", totalAmount, expiresAt}`; HTTP 201.
- Trong **một** transaction: khóa hold (`UPDLOCK`), kiểm tra owner/suất/tập ghế/còn hạn → ghế `HELD` chuyển owner từ hold sang booking (update có điều kiện, kiểm tra số dòng = số ghế) → hold `CONSUMED`, xóa `ActiveHold` → insert `Booking` (channel `CUSTOMER`, `expires_at = now + 5 phút`) + `Booking_Detail` lưu snapshot tên/giá → voucher: `UPDATE Voucher SET reserved_count += 1 WHERE used_count + reserved_count < usage_limit` (0 dòng → 409 `VOUCHER_EXHAUSTED`) + `Voucher_Reservation`.

- [ ] **Step 1: Test fail:**
  - `create_happyPath_seatsOwnedByBooking_holdConsumed_expiresIn5Minutes`.
  - `create_expiredHold_409_noBookingRow`.
  - `create_twoParallelRequestsSameHold_exactlyOneBooking` (2 thread + `CountDownLatch`).
  - `voucherLastQuota_twoParallelCustomers_oneDiscounted_other409`.
  - `catalogDown_503_noBookingRow`.
- [ ] **Step 2:** Test → FAIL.
- [ ] **Step 3:** Cài đặt.
- [ ] **Step 4:** Test → PASS.
- [ ] **Step 5:** Commit `feat(booking): create booking atomically from an active seat hold`.

### Task 3: Prevent Duplicate Submission (Idempotency)

**Files:**
- Create: `.../booking/service/IdempotencyService.java`, `.../booking/entity/IdempotencyRecord.java` + `repository/IdempotencyRecordRepository.java`, `.../booking/service/RequestHasher.java`
- Create: `.../booking/service/IdempotencyCleanupJob.java` (xóa bản ghi quá 24 giờ)
- Modify: `CustomerBookingController.java` (đọc header `Idempotency-Key`)
- Test: `.../service/RequestHasherTest.java`, `.../service/CreateBookingIdempotencyIT.java`

**Interfaces:**
- `RequestHasher.hash(String operation, CustomerBookingRequest req): String` — SHA-256 trên payload đã chuẩn hóa: sort `showtimeSeatIds`, gộp + sort concessions, voucher trim/upper, kèm `operation` + version.
- Header thiếu/sai định dạng → 400 `IDEMPOTENCY_KEY_INVALID`. Cùng key + cùng payload → **200** trả lại đúng body cũ. Khác payload → 409 `IDEMPOTENCY_KEY_REUSED`. Đang xử lý → 409 `REQUEST_IN_PROGRESS` + `Retry-After: 1`.
- Bản ghi idempotency insert trong **cùng transaction** với booking (unique constraint quyết định ai thắng).

- [ ] **Step 1: Test fail:**
  - `RequestHasherTest.seatOrderAndDuplicateConcessionLines_sameHash`.
  - `sameKeySamePayload_secondCall200_sameBookingId_oneBookingRow`.
  - `sameKeyDifferentPayload_409_IDEMPOTENCY_KEY_REUSED`.
  - `twoParallelSameKey_oneBookingRow`.
  - `replayAfterNewApplicationContext_still200` (mô phỏng restart).
- [ ] **Step 2–4:** FAIL → cài đặt → PASS.
- [ ] **Step 5:** Commit `feat(booking): durable idempotency for create booking`.

### Task 4: View Booked Detail

**Files:**
- Modify: `CustomerBookingController.java` (`GET /api/customer/bookings/{bookingId}`)
- Create: `.../booking/dto/BookingDetailResponse.java`
- Test: `.../controller/BookingQueryWebTest.java`

**Interfaces:**
- Response: `{bookingId, status, createdAt, expiresAt, movieTitle, roomName, showDate, startTime, seats[{label, seatType, unitPrice}], concessions[{name, quantity, unitPrice, lineTotal}], voucherCode, discountAmount, totalAmount, payment{paymentId, status, paidAt}?, ticketId?}` — lấy từ snapshot, **không** gọi cinema/catalog.
- Đơn của người khác → 403 `BOOKING_NOT_OWNED`; không tồn tại → 404.

- [ ] **Step 1: Test fail:** `detail_own_200_usesSnapshotPrices_evenIfCatalogPriceChanged`; `detail_otherCustomer_403`; `detail_unknown_404`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS.
- [ ] **Step 5:** Commit `feat(booking): view own booking detail`.

### Task 5: View My Booked

**Files:**
- Modify: `CustomerBookingController.java` (`GET /api/customer/bookings`)
- Create: `.../booking/dto/BookingSummaryItem.java`, `PageResponse.java`
- Test: thêm vào `BookingQueryWebTest.java`

**Interfaces:**
- Query: `status?` (một trong 4 trạng thái), `page` (từ 0), `size` (mặc định 20, tối đa 100). Sắp xếp `createdAt` giảm dần.
- Response: `{items[{bookingId, status, movieTitle, showDate, startTime, seatLabels[], totalAmount, createdAt, expiresAt}], page, size, totalElements, totalPages}`.

- [ ] **Step 1: Test fail:** `list_returnsOnlyCallerBookings`; `list_filterPaid`; `list_size101_400`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS.
- [ ] **Step 5:** Commit `feat(booking): list own bookings with paging and status filter`.

### Task 6: Cancel Eligible Booking (+ tự hết hạn)

**Files:**
- Modify: `CustomerBookingController.java` (`POST /api/customer/bookings/{bookingId}/cancel`)
- Create: `.../booking/service/BookingReleaseService.java` (nhả ghế + voucher, dùng chung cho hủy, hết hạn, payment FAILED)
- Create: `.../booking/service/BookingExpiryJob.java` (`@Scheduled` mỗi 30 giây)
- Test: `.../service/CancelAndExpiryIT.java`

**Interfaces:**
- `BookingReleaseService.release(String bookingId, BookingStatus target)` — chỉ chạy khi `UPDATE Booking SET status=? WHERE booking_id=? AND status='PENDING_PAYMENT'` trả về 1 dòng; nhả đúng ghế `booking_id = ?` về `AVAILABLE`; `Voucher_Reservation` đánh dấu đã hoàn, `reserved_count -= 1` đúng một lần.
- Hủy: chỉ đơn `PENDING_PAYMENT` của chính mình → 200 `{bookingId, status:"CANCELLED"}`; đã hủy → 200 trạng thái hiện tại (không nhả lần 2); đã `PAID`/`EXPIRED` → 409 `BOOKING_NOT_CANCELLABLE`.
- Job: đơn `PENDING_PAYMENT` có `expires_at <= now` → `EXPIRED` qua cùng `release`.

- [ ] **Step 1: Test fail:**
  - `cancel_pending_seatsAvailable_andCanBeHeldAgain`.
  - `cancelTwice_voucherReservedCountDecrementedOnce`.
  - `cancel_paid_409`.
  - `expiryJob_expiresOnlyOverdue_releasesOnlyItsOwnSeats`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS.
- [ ] **Step 5:** Commit `feat(booking): cancel pending bookings and expire unpaid ones`.

### Task 7: Process Demo Payment

**Files:**
- Create: `.../booking/controller/DemoPaymentController.java` (`POST /api/customer/bookings/{bookingId}/demo-payment`, chỉ tạo bean khi `booking.demo-payment.enabled=true`)
- Create: `.../booking/service/PaymentService.java`, `entity/Payment.java`, `entity/Ticket.java` + repository
- Modify: `IdempotencyService` (operation `demo-payment`)
- Test: `.../service/DemoPaymentIT.java`

**Interfaces:**
- Body `{outcome: "SUCCESS"|"FAILED"}`, header `Idempotency-Key` bắt buộc.
- SUCCESS, trong một transaction: khóa booking, phải `PENDING_PAYMENT` và `expires_at > now` → `Payment SUCCEEDED` (amount = `Booking.total_amount`) → booking `PAID` → ghế `SOLD` → voucher `reserved_count -1, used_count +1` → tạo `Ticket` (UUID, `checked_in=0`).
- FAILED: `Payment FAILED` + `BookingReleaseService.release(id, CANCELLED)`; sau đó gửi SUCCESS → 409 `BOOKING_NOT_PAYABLE` (phải đặt đơn mới).
- Response `{bookingId, paymentId, paymentStatus, amount, ticketId?, message}`.

- [ ] **Step 1: Test fail:**
  - `success_paidSoldAndExactlyOneTicket`.
  - `sameKeyTwice_oneTicket_secondCallReplays200`.
  - `failed_releasesSeats_thenSuccess409`.
  - `afterExpiresAt_409_noTicket`.
  - `paymentAndCancelInParallel_exactlyOneFinalState`.
  - `demoDisabled_404`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS.
- [ ] **Step 5:** Commit `feat(booking): demo payment issues one ticket atomically`.

### Task 8: Show Server Issued Ticket

**Files:**
- Create: `.../booking/controller/CustomerTicketController.java` (`GET /api/customer/tickets`, `GET /api/customer/tickets/{ticketId}`)
- Create: `.../booking/dto/TicketResponse.java`
- Test: `.../controller/TicketQueryWebTest.java`

**Interfaces:**
- Response: `{ticketId, bookingId, movieTitle, roomName, showDate, startTime, seatLabels[], status (VALID|CHECKED_IN), checkedInAt?, qrPayload}`; `qrPayload = "CINEMA-TICKET:" + ticketId` (chỉ ID do server cấp, không chứa dữ liệu cá nhân).
- Chỉ trả vé của booking `PAID` thuộc khách gọi; vé người khác → 403.

- [ ] **Step 1: Test fail:** `tickets_onlyAfterPaid`; `ticketOfAnotherCustomer_403`; `qrPayloadContainsServerTicketId`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS. Chạy Flutter: màn "Vé của tôi" hiện mã QR.
- [ ] **Step 5:** Commit `feat(booking): customer views server-issued tickets`.

### Task 9: Check-in Ticket For Admin

**Files:**
- Create: `.../booking/controller/AdminTicketController.java` (`POST /api/v1/admin/tickets/{ticketId}/check-in`)
- Modify: `docs/postman/cinema-gateway.postman_collection.json` (thêm luồng tạo đơn → thanh toán → check-in)
- Test: `.../service/CheckInIT.java`

**Interfaces:**
- Chỉ `ADMIN` (`@PreAuthorize` tại service, gateway cũng chặn). `checked_in_staff_id` lấy từ claim `staffId`.
- `UPDATE Ticket SET checked_in=1, checked_in_time=now, checked_in_staff_id=? WHERE ticket_id=? AND checked_in=0` + kiểm tra booking `PAID`; 0 dòng → 409 `TICKET_ALREADY_CHECKED_IN`.
- Response 200 `{ticketId, checkedInAt, checkedInBy}`.

- [ ] **Step 1: Test fail:** `checkIn_twiceInParallel_oneSuccess_one409`; `userToken_403`; `ticketOfCancelledBooking_409`.
- [ ] **Step 2–4:** FAIL → cài đặt → PASS. Postman: chạy cả collection → toàn bộ PASS.
- [ ] **Step 5:** Commit `feat(booking): admin ticket check-in with single-use guarantee`.

## Kiểm chứng cuối

- `.\mvnw.cmd verify` ở gốc repo → `BUILD SUCCESS`, có `Tests run` cho booking-service, IT chạy thật trên SQL Server container (không SKIPPED).
- `cd cinema_booking_app; flutter analyze; flutter test`.
- Chạy app Flutter (`--dart-define=USE_CONCESSION_API=true`) qua gateway: chọn ghế → xem tóm tắt → tạo đơn → thanh toán demo → xem vé QR → admin check-in bằng Postman.
- Ghi kết quả thực tế vào `docs/verification.md`.

## Ngoài phạm vi plan này

- API giữ ghế (`POST/DELETE /api/customer/seat-holds`) — chờ D2.
- Workflow publish/close bán vé từ cinema-service (AGENTS §4) — thay script ETL ở Task 0 khi cinema-service có API.
- Thanh toán thật qua cổng ngân hàng, hoàn tiền, đổi vé.
