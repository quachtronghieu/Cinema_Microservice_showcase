-- DEV ONLY: seed booking_db from the legacy Cinema_Management1 database on the same SQL Server instance.
-- Run after booking-service applied Flyway V1. Safe to re-run: existing IDs are skipped.
-- Stand-in for Phan Duc Tho's "Open / Close Showtime Sales" until cinema-service pushes snapshots through its internal API.
--
-- Rules (see docs/superpowers/plans/2026-11-04-bookings-and-payment.md, D4):
--   * Seat price comes from the legacy SeatPricingService constants: STANDARD 80000, VIP 100000 VND.
--   * Legacy bookings are NOT migrated here (Data Migration task, Iteration 3), so every imported seat starts AVAILABLE.
--   * @openAll = 1 opens every imported showtime for local testing, including past ones (all legacy showtimes are past).
--   * Legacy rooms have no name: room_name = 'Room <id>'.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @openAll BIT = 1;

BEGIN TRAN;

INSERT INTO booking_db.dbo.Sales_Snapshot
    (showtime_id, snapshot_version, sales_status, movie_id, movie_title, room_id, room_name, show_date, start_time, end_time)
SELECT s.showtime_id, 1,
       CASE WHEN @openAll = 1 OR s.show_date >= CAST(SYSDATETIME() AS DATE) THEN 'OPEN' ELSE 'CLOSED' END,
       s.movie_id, m.title, s.room_id, CONCAT(N'Room ', s.room_id), s.show_date,
       CAST(s.start_time AS TIME(0)), CAST(s.end_time AS TIME(0))
FROM Cinema_Management1.dbo.Showtime s
JOIN Cinema_Management1.dbo.Movie m ON m.movie_id = s.movie_id
WHERE NOT EXISTS (SELECT 1 FROM booking_db.dbo.Sales_Snapshot x WHERE x.showtime_id = s.showtime_id COLLATE DATABASE_DEFAULT);
SELECT @@ROWCOUNT AS showtimes_imported;

INSERT INTO booking_db.dbo.Showtime_Seat
    (showtime_seat_id, showtime_id, template_seat_id, row_label, seat_number, seat_type, unit_price, status)
SELECT ss.showtime_seat_id, ss.showtime_id, ss.template_seat_id, ts.row_label, ts.seat_number,
       UPPER(ts.seat_type),
       CASE UPPER(ts.seat_type) WHEN 'VIP' THEN 100000 ELSE 80000 END,
       'AVAILABLE'
FROM Cinema_Management1.dbo.Showtime_Seat ss
JOIN Cinema_Management1.dbo.Template_Seat ts ON ts.template_seat_id = ss.template_seat_id
WHERE UPPER(ts.seat_type) IN ('STANDARD', 'VIP')
  AND EXISTS (SELECT 1 FROM booking_db.dbo.Sales_Snapshot x WHERE x.showtime_id = ss.showtime_id COLLATE DATABASE_DEFAULT)
  AND NOT EXISTS (SELECT 1 FROM booking_db.dbo.Showtime_Seat y WHERE y.showtime_seat_id = ss.showtime_seat_id COLLATE DATABASE_DEFAULT);
SELECT @@ROWCOUNT AS seats_imported;

INSERT INTO booking_db.dbo.Voucher
    (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status)
SELECT v.voucher_id, v.code, v.voucher_name,
       CASE v.discount_type WHEN 'Percentage' THEN 'PERCENTAGE' ELSE 'FIXED' END,
       v.discount_value, v.usage_limit, v.used_count, 0, v.status
FROM Cinema_Management1.dbo.Voucher v
WHERE v.discount_type IN ('Percentage', 'Amount')
  AND NOT EXISTS (SELECT 1 FROM booking_db.dbo.Voucher y WHERE y.voucher_id = v.voucher_id COLLATE DATABASE_DEFAULT);
SELECT @@ROWCOUNT AS vouchers_imported;

COMMIT;
