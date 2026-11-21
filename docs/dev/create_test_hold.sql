-- DEV ONLY: create a seat hold directly in booking_db so booking flows can be tested
-- before Nguyen Dao Thu Ngan's seat-hold API (POST /api/v1/customer/seat-holds) exists.
-- Writes the same tables that API will own: SeatHold, SeatHold_Item, ActiveHold, Showtime_Seat.
-- Usage: sqlcmd -S localhost -E -C -d booking_db -i docs/dev/create_test_hold.sql
--        -v customerId="CS000003" showtimeId="SW00002" seatCount=2 minutes=30
-- Prints holdId, showtimeId and showtimeSeatIds for Postman or the Flutter /booking-summary-debug screen.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @customerId VARCHAR(36) = '$(customerId)';
DECLARE @showtimeId VARCHAR(36) = '$(showtimeId)';
DECLARE @seatCount INT = $(seatCount);
DECLARE @minutes INT = $(minutes);
DECLARE @now DATETIME2(3) = SYSUTCDATETIME();
DECLARE @holdId VARCHAR(36) = LOWER(CONVERT(VARCHAR(36), NEWID()));

IF @seatCount NOT BETWEEN 1 AND 8 THROW 50001, 'seatCount must be between 1 and 8.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.Sales_Snapshot WHERE showtime_id = @showtimeId AND sales_status = 'OPEN')
    THROW 50002, 'Showtime not found or not open for sales. Run docs/migration/booking_sales_snapshot_import.sql first.', 1;

BEGIN TRAN;

-- Replace this customer's previous hold for the same showtime (one active hold per customer and showtime).
DECLARE @oldHold VARCHAR(36) = (SELECT hold_id FROM dbo.ActiveHold WITH (UPDLOCK, HOLDLOCK)
                                WHERE customer_id = @customerId AND showtime_id = @showtimeId);
IF @oldHold IS NOT NULL
BEGIN
    DELETE FROM dbo.ActiveHold WHERE customer_id = @customerId AND showtime_id = @showtimeId;
    UPDATE dbo.Showtime_Seat SET status = 'AVAILABLE', hold_id = NULL, version = version + 1
    WHERE hold_id = @oldHold AND status = 'HELD';
    UPDATE dbo.SeatHold SET status = 'RELEASED', version = version + 1 WHERE hold_id = @oldHold AND status = 'ACTIVE';
END;

DECLARE @seats TABLE (showtime_seat_id VARCHAR(36) PRIMARY KEY);
INSERT INTO @seats
SELECT TOP (@seatCount) showtime_seat_id
FROM dbo.Showtime_Seat WITH (UPDLOCK, READPAST)
WHERE showtime_id = @showtimeId AND status = 'AVAILABLE'
ORDER BY row_label, seat_number;

IF (SELECT COUNT(*) FROM @seats) < @seatCount THROW 50003, 'Not enough available seats in this showtime.', 1;

INSERT INTO dbo.SeatHold (hold_id, customer_id, showtime_id, status, created_at, expires_at)
VALUES (@holdId, @customerId, @showtimeId, 'ACTIVE', @now, DATEADD(MINUTE, @minutes, @now));

UPDATE s SET status = 'HELD', hold_id = @holdId, version = version + 1
FROM dbo.Showtime_Seat s JOIN @seats x ON x.showtime_seat_id = s.showtime_seat_id
WHERE s.status = 'AVAILABLE';
IF @@ROWCOUNT <> @seatCount THROW 50004, 'Seat state changed concurrently; run again.', 1;

INSERT INTO dbo.SeatHold_Item (hold_id, showtime_seat_id) SELECT @holdId, showtime_seat_id FROM @seats;
INSERT INTO dbo.ActiveHold (customer_id, showtime_id, hold_id) VALUES (@customerId, @showtimeId, @holdId);

COMMIT;

SELECT @holdId AS holdId, @showtimeId AS showtimeId,
       (SELECT STRING_AGG(showtime_seat_id, ',') FROM @seats) AS showtimeSeatIds,
       DATEADD(MINUTE, @minutes, @now) AS expiresAtUtc;
