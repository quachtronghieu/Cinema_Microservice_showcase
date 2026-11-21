-- LOCAL/DEV. Read only. Reconciliation report of the 4 databases (B-06, docs/ngan-task-scope.md).
-- Needs SQLCMD mode (SSMS: Query > SQLCMD Mode; or sqlcmd -i). SUFFIX "" checks the originals,
-- SUFFIX "_restore" checks the copies made by 02_restore_as_copy.sql.
-- Cross-database reads happen only in this report, never in a service (AGENTS section 3).
-- Result 1: rows per table. Result 2: checks; ERROR rows must have problems = 0, WARN rows need a human look.
-- Result 3: status counts. Result 4: money totals. Compare results 1, 3, 4 between the originals and the copies.
:setvar SUFFIX ""
:on error exit
SET NOCOUNT ON;
IF DB_ID(N'identity_db$(SUFFIX)') IS NULL OR DB_ID(N'catalog_db$(SUFFIX)') IS NULL
   OR DB_ID(N'cinema_db$(SUFFIX)') IS NULL OR DB_ID(N'booking_db$(SUFFIX)') IS NULL
    THROW 51531, 'One of the 4 databases with this SUFFIX does not exist.', 1;
GO

-- 1. Rows per table
IF OBJECT_ID('tempdb..#rows') IS NOT NULL DROP TABLE #rows;
CREATE TABLE #rows (database_name SYSNAME, table_name NVARCHAR(300), row_count BIGINT);
DECLARE @dbs TABLE (ord INT, name SYSNAME);
INSERT @dbs VALUES (1, N'identity_db$(SUFFIX)'), (2, N'catalog_db$(SUFFIX)'), (3, N'cinema_db$(SUFFIX)'), (4, N'booking_db$(SUFFIX)');
DECLARE @ord INT = 1, @db SYSNAME, @sql NVARCHAR(MAX);
WHILE @ord <= 4
BEGIN
    SELECT @db = name FROM @dbs WHERE ord = @ord;
    SET @sql = N'SELECT @db, QUOTENAME(s.name) + ''.'' + QUOTENAME(t.name), SUM(p.rows) FROM ' + QUOTENAME(@db) + N'.sys.tables t '
             + N'JOIN ' + QUOTENAME(@db) + N'.sys.schemas s ON s.schema_id = t.schema_id '
             + N'JOIN ' + QUOTENAME(@db) + N'.sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1) '
             + N'WHERE t.is_ms_shipped = 0 GROUP BY s.name, t.name;';
    INSERT #rows EXEC sys.sp_executesql @sql, N'@db SYSNAME', @db = @db;
    SET @ord += 1;
END;
SELECT database_name, table_name, row_count FROM #rows ORDER BY database_name, table_name;
GO

-- 2. Checks. Each check lists up to 5 sample IDs.
IF OBJECT_ID('tempdb..#r') IS NOT NULL DROP TABLE #r;
CREATE TABLE #r (code VARCHAR(4), severity VARCHAR(5), area VARCHAR(10), check_text NVARCHAR(200), problems INT, sample NVARCHAR(1000));
GO

-- Identity
WITH q AS (SELECT p.principal_id AS id FROM [identity_db$(SUFFIX)].dbo.Principal p
           WHERE NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Principal_Role r WHERE r.principal_id = p.principal_id))
INSERT #r SELECT 'I01', 'ERROR', 'identity', N'Principal without any role', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT c.user_id AS id FROM [identity_db$(SUFFIX)].dbo.Customer c
           WHERE NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Principal p WHERE p.customer_id = c.user_id))
INSERT #r SELECT 'I02', 'WARN', 'identity', N'Customer without a login (Principal)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT principal_id AS id FROM [identity_db$(SUFFIX)].dbo.Principal
           WHERE (account_type = 'CUSTOMER' AND (customer_id IS NULL OR staff_id IS NOT NULL))
              OR (account_type = 'STAFF' AND (staff_id IS NULL OR customer_id IS NOT NULL)))
INSERT #r SELECT 'I03', 'ERROR', 'identity', N'Principal whose account type does not match customer_id/staff_id', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

INSERT #r SELECT 'I04', 'ERROR', 'identity', N'No active ADMIN login for the demo',
    CASE WHEN EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Principal p JOIN [identity_db$(SUFFIX)].dbo.Principal_Role r
                      ON r.principal_id = p.principal_id WHERE p.is_active = 1 AND r.role_name = 'ADMIN' AND p.staff_id IS NOT NULL)
         THEN 0 ELSE 1 END, NULL;

INSERT #r SELECT 'I05', 'ERROR', 'identity', N'No active USER login with customerId for the demo',
    CASE WHEN EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Principal p JOIN [identity_db$(SUFFIX)].dbo.Principal_Role r
                      ON r.principal_id = p.principal_id WHERE p.is_active = 1 AND r.role_name = 'USER' AND p.customer_id IS NOT NULL)
         THEN 0 ELSE 1 END, NULL;

WITH q AS (SELECT DISTINCT p.username AS id FROM [identity_db$(SUFFIX)].dbo.Principal p
           JOIN [identity_db$(SUFFIX)].dbo.Principal_Role r ON r.principal_id = p.principal_id
           WHERE p.is_active = 1 AND r.role_name NOT IN ('USER', 'ADMIN'))
INSERT #r SELECT 'I06', 'WARN', 'identity', N'Active login with a legacy role (scope 1.4 keeps only USER/ADMIN)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Catalog
WITH q AS (SELECT CONCAT(w.customer_id, '/', w.movie_id COLLATE DATABASE_DEFAULT) AS id FROM [catalog_db$(SUFFIX)].dbo.Watchlist w
           WHERE NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Customer c WHERE c.user_id = w.customer_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'K01', 'WARN', 'catalog', N'Watchlist row whose customer is not in identity', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Cinema
WITH q AS (SELECT r.room_id AS id FROM [cinema_db$(SUFFIX)].dbo.Room r
           WHERE NOT EXISTS (SELECT 1 FROM [cinema_db$(SUFFIX)].dbo.Template_Seat s WHERE s.template_id = r.template_id))
INSERT #r SELECT 'C01', 'ERROR', 'cinema', N'Room whose template has no seats', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT s.showtime_id AS id FROM [cinema_db$(SUFFIX)].dbo.Showtime s
           WHERE NOT EXISTS (SELECT 1 FROM [catalog_db$(SUFFIX)].dbo.Movie m WHERE m.movie_id = s.movie_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'C02', 'ERROR', 'cinema', N'Showtime whose movie is not in catalog', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

-- Effective start/end: show date + time, end moved to the next day when it is not after the start (midnight crossing).
WITH t AS (SELECT showtime_id, room_id, sales_status,
                  CAST(CONCAT(show_date, ' ', start_time) AS DATETIME2(0)) AS st,
                  DATEADD(DAY, CASE WHEN end_time > start_time THEN 0 ELSE 1 END, CAST(CONCAT(show_date, ' ', end_time) AS DATETIME2(0))) AS en
           FROM [cinema_db$(SUFFIX)].dbo.Showtime),
     q AS (SELECT CONCAT(a.showtime_id, '/', b.showtime_id) AS id FROM t a JOIN t b
           ON a.room_id = b.room_id AND a.showtime_id < b.showtime_id AND a.st < b.en AND b.st < a.en)
INSERT #r SELECT 'C03', 'WARN', 'cinema', N'Two showtimes overlapping in the same room (midnight crossing included)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) x);

-- Decision D-05: rooms can go INACTIVE while showtimes more than one month ahead remain; an OPEN one keeps selling.
WITH q AS (SELECT s.showtime_id AS id FROM [cinema_db$(SUFFIX)].dbo.Showtime s JOIN [cinema_db$(SUFFIX)].dbo.Room r ON r.room_id = s.room_id
           WHERE r.status = 'INACTIVE' AND s.sales_status = 'OPEN' AND s.show_date >= CAST(GETDATE() AS DATE))
INSERT #r SELECT 'C04', 'WARN', 'cinema', N'OPEN upcoming showtime in an INACTIVE room', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Booking against cinema and catalog
WITH q AS (SELECT x.showtime_id AS id FROM [booking_db$(SUFFIX)].dbo.Sales_Snapshot x
           WHERE NOT EXISTS (SELECT 1 FROM [cinema_db$(SUFFIX)].dbo.Showtime s WHERE s.showtime_id = x.showtime_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'B01', 'ERROR', 'booking', N'Sales snapshot whose showtime is not in cinema', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT x.showtime_id AS id FROM [booking_db$(SUFFIX)].dbo.Sales_Snapshot x
           JOIN [cinema_db$(SUFFIX)].dbo.Showtime s ON s.showtime_id = x.showtime_id COLLATE Latin1_General_100_BIN2
           WHERE s.movie_id <> x.movie_id COLLATE Latin1_General_100_BIN2 OR s.room_id <> x.room_id
              OR s.show_date <> x.show_date OR s.start_time <> x.start_time OR s.end_time <> x.end_time)
INSERT #r SELECT 'B02', 'WARN', 'booking', N'Sales snapshot differs from the cinema showtime (movie, room, date or time)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT x.showtime_id AS id FROM [booking_db$(SUFFIX)].dbo.Sales_Snapshot x
           WHERE NOT EXISTS (SELECT 1 FROM [catalog_db$(SUFFIX)].dbo.Movie m WHERE m.movie_id = x.movie_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'B03', 'ERROR', 'booking', N'Sales snapshot whose movie is not in catalog', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT ss.showtime_seat_id AS id FROM [booking_db$(SUFFIX)].dbo.Showtime_Seat ss
           JOIN [booking_db$(SUFFIX)].dbo.Sales_Snapshot x ON x.showtime_id = ss.showtime_id
           LEFT JOIN [cinema_db$(SUFFIX)].dbo.Room r ON r.room_id = x.room_id
           LEFT JOIN [cinema_db$(SUFFIX)].dbo.Template_Seat t
               ON t.template_seat_id = ss.template_seat_id COLLATE Latin1_General_100_BIN2 AND t.template_id = r.template_id
           WHERE t.template_seat_id IS NULL OR t.row_label <> ss.row_label COLLATE Latin1_General_100_BIN2
              OR t.seat_number <> ss.seat_number OR t.seat_type <> ss.seat_type COLLATE Latin1_General_100_BIN2)
INSERT #r SELECT 'B04', 'ERROR', 'booking', N'Showtime seat not matching a seat of the room template (ID, row, number, type)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT x.showtime_id AS id FROM [booking_db$(SUFFIX)].dbo.Sales_Snapshot x
           LEFT JOIN [cinema_db$(SUFFIX)].dbo.Room r ON r.room_id = x.room_id
           WHERE (SELECT COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Showtime_Seat ss WHERE ss.showtime_id = x.showtime_id)
              <> (SELECT COUNT(*) FROM [cinema_db$(SUFFIX)].dbo.Template_Seat t WHERE t.template_id = r.template_id))
INSERT #r SELECT 'B05', 'ERROR', 'booking', N'Showtime whose seat count differs from its room template', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Booking: seat ownership and holds
WITH q AS (SELECT s.showtime_seat_id AS id FROM [booking_db$(SUFFIX)].dbo.Showtime_Seat s
           LEFT JOIN [booking_db$(SUFFIX)].dbo.SeatHold h ON h.hold_id = s.hold_id
           LEFT JOIN [booking_db$(SUFFIX)].dbo.Booking b ON b.booking_id = s.booking_id
           WHERE (s.status = 'HELD' AND s.hold_id IS NOT NULL AND (h.status <> 'ACTIVE' OR h.showtime_id <> s.showtime_id))
              OR (s.status = 'HELD' AND s.booking_id IS NOT NULL AND (b.status <> 'PENDING_PAYMENT' OR b.showtime_id <> s.showtime_id))
              OR (s.status = 'SOLD' AND (b.status <> 'PAID' OR b.showtime_id <> s.showtime_id)))
INSERT #r SELECT 'B06', 'ERROR', 'booking', N'HELD/SOLD seat whose owner hold or booking is in the wrong state or showtime', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT a.hold_id AS id FROM [booking_db$(SUFFIX)].dbo.ActiveHold a
           JOIN [booking_db$(SUFFIX)].dbo.SeatHold h ON h.hold_id = a.hold_id WHERE h.status <> 'ACTIVE'
           UNION ALL
           SELECT h.hold_id FROM [booking_db$(SUFFIX)].dbo.SeatHold h WHERE h.status = 'ACTIVE'
             AND NOT EXISTS (SELECT 1 FROM [booking_db$(SUFFIX)].dbo.ActiveHold a WHERE a.hold_id = h.hold_id))
INSERT #r SELECT 'B07', 'ERROR', 'booking', N'ActiveHold slot and ACTIVE hold do not match', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT DISTINCT h.hold_id AS id FROM [booking_db$(SUFFIX)].dbo.SeatHold h
           JOIN [booking_db$(SUFFIX)].dbo.SeatHold_Item i ON i.hold_id = h.hold_id
           JOIN [booking_db$(SUFFIX)].dbo.Showtime_Seat s ON s.showtime_seat_id = i.showtime_seat_id
           WHERE h.status = 'ACTIVE' AND (s.status <> 'HELD' OR s.hold_id IS NULL OR s.hold_id <> h.hold_id))
INSERT #r SELECT 'B08', 'ERROR', 'booking', N'ACTIVE hold listing a seat it does not hold', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT hold_id AS id FROM [booking_db$(SUFFIX)].dbo.SeatHold
           WHERE status = 'ACTIVE' AND expires_at < DATEADD(MINUTE, -1, SYSUTCDATETIME()))
INSERT #r SELECT 'B09', 'WARN', 'booking', N'ACTIVE hold expired more than 1 minute ago (expiry job not run yet, e.g. service stopped)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT DISTINCT b.booking_id AS id FROM [booking_db$(SUFFIX)].dbo.Booking b
           JOIN [booking_db$(SUFFIX)].dbo.Booking_Detail d ON d.booking_id = b.booking_id AND d.item_type = 'SEAT'
           LEFT JOIN [booking_db$(SUFFIX)].dbo.Showtime_Seat s ON s.showtime_seat_id = d.showtime_seat_id
           WHERE b.status IN ('PENDING_PAYMENT', 'PAID')
             AND (s.showtime_id IS NULL OR s.showtime_id <> b.showtime_id OR s.booking_id IS NULL OR s.booking_id <> b.booking_id
                  OR s.status <> CASE b.status WHEN 'PAID' THEN 'SOLD' ELSE 'HELD' END))
INSERT #r SELECT 'B10', 'ERROR', 'booking', N'Pending or paid booking whose seat lines are not seats it owns', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Booking: money, payment, ticket, voucher
WITH q AS (SELECT b.booking_id AS id FROM [booking_db$(SUFFIX)].dbo.Booking b
           WHERE b.subtotal <> ISNULL((SELECT SUM(d.total_price) FROM [booking_db$(SUFFIX)].dbo.Booking_Detail d WHERE d.booking_id = b.booking_id), 0)
              OR b.total_amount <> b.subtotal - b.discount_amount)
INSERT #r SELECT 'B11', 'ERROR', 'booking', N'Booking subtotal/total not equal to its lines minus discount', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT b.booking_id AS id FROM [booking_db$(SUFFIX)].dbo.Booking b
           LEFT JOIN [booking_db$(SUFFIX)].dbo.Payment p ON p.booking_id = b.booking_id
           LEFT JOIN [booking_db$(SUFFIX)].dbo.Ticket t ON t.booking_id = b.booking_id
           WHERE (b.status = 'PAID' AND (p.status IS NULL OR p.status <> 'SUCCEEDED' OR p.amount <> b.total_amount OR t.ticket_id IS NULL))
              OR (b.status <> 'PAID' AND (p.status = 'SUCCEEDED' OR t.ticket_id IS NOT NULL)))
INSERT #r SELECT 'B12', 'ERROR', 'booking', N'PAID booking without a matching succeeded payment and ticket, or the reverse', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT v.code AS id FROM [booking_db$(SUFFIX)].dbo.Voucher v
           WHERE v.used_count <> (SELECT COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Voucher_Reservation r WHERE r.voucher_id = v.voucher_id AND r.status = 'CONSUMED')
              OR v.reserved_count <> (SELECT COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Voucher_Reservation r WHERE r.voucher_id = v.voucher_id AND r.status = 'RESERVED'))
INSERT #r SELECT 'B13', 'WARN', 'booking', N'Voucher used/reserved counts differ from its reservations (legacy counts are expected here)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

-- Booking against identity and catalog
WITH q AS (SELECT DISTINCT b.customer_id AS id FROM [booking_db$(SUFFIX)].dbo.Booking b
           WHERE NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Customer c WHERE c.user_id = b.customer_id COLLATE Latin1_General_100_BIN2)
           UNION
           SELECT h.customer_id FROM [booking_db$(SUFFIX)].dbo.SeatHold h
           WHERE NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Customer c WHERE c.user_id = h.customer_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'B14', 'ERROR', 'booking', N'Hold or booking owner not in identity Customer', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT DISTINCT d.concession_id AS id FROM [booking_db$(SUFFIX)].dbo.Booking_Detail d
           WHERE d.item_type = 'CONCESSION'
             AND NOT EXISTS (SELECT 1 FROM [catalog_db$(SUFFIX)].dbo.Concession c WHERE c.concession_id = d.concession_id COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'B15', 'WARN', 'booking', N'Concession line whose item is no longer in catalog (name/price kept as snapshot)', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);

WITH q AS (SELECT t.ticket_id AS id FROM [booking_db$(SUFFIX)].dbo.Ticket t
           WHERE t.checked_in = 1 AND NOT EXISTS (SELECT 1 FROM [identity_db$(SUFFIX)].dbo.Staff s WHERE s.staff_id = t.checked_in_by COLLATE Latin1_General_100_BIN2))
INSERT #r SELECT 'B16', 'WARN', 'booking', N'Checked-in ticket whose staffId is not in identity Staff', (SELECT COUNT(*) FROM q),
    (SELECT STRING_AGG(CONVERT(NVARCHAR(100), id), ', ') FROM (SELECT TOP 5 id FROM q ORDER BY id) t);
GO

SELECT code, severity, area, check_text, problems, sample,
       CASE WHEN problems = 0 THEN 'OK' WHEN severity = 'ERROR' THEN 'FAIL' ELSE 'CHECK' END AS result
FROM #r ORDER BY code;
SELECT SUM(CASE WHEN severity = 'ERROR' AND problems > 0 THEN 1 ELSE 0 END) AS failed_error_checks,
       SUM(CASE WHEN severity = 'WARN' AND problems > 0 THEN 1 ELSE 0 END) AS warn_checks_to_review
FROM #r;
GO

-- 3. Status counts
SELECT 'cinema.Room' AS item, status AS state, COUNT(*) AS n FROM [cinema_db$(SUFFIX)].dbo.Room GROUP BY status
UNION ALL SELECT 'cinema.Showtime', sales_status, COUNT(*) FROM [cinema_db$(SUFFIX)].dbo.Showtime GROUP BY sales_status
UNION ALL SELECT 'booking.Sales_Snapshot', sales_status, COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Sales_Snapshot GROUP BY sales_status
UNION ALL SELECT 'booking.Showtime_Seat', status, COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Showtime_Seat GROUP BY status
UNION ALL SELECT 'booking.SeatHold', status, COUNT(*) FROM [booking_db$(SUFFIX)].dbo.SeatHold GROUP BY status
UNION ALL SELECT 'booking.Booking', status, COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Booking GROUP BY status
UNION ALL SELECT 'booking.Payment', status, COUNT(*) FROM [booking_db$(SUFFIX)].dbo.Payment GROUP BY status
UNION ALL SELECT 'booking.Ticket', CASE checked_in WHEN 1 THEN 'CHECKED_IN' ELSE 'NOT_CHECKED_IN' END, COUNT(*)
          FROM [booking_db$(SUFFIX)].dbo.Ticket GROUP BY checked_in
UNION ALL SELECT 'identity.Principal_Role', role_name, COUNT(*) FROM [identity_db$(SUFFIX)].dbo.Principal_Role GROUP BY role_name
ORDER BY item, state;

-- 4. Money totals (VND)
SELECT 'Booking.total_amount' AS item, status AS state, COUNT(*) AS n, SUM(total_amount) AS amount
FROM [booking_db$(SUFFIX)].dbo.Booking GROUP BY status
UNION ALL SELECT 'Booking_Detail.total_price', item_type, COUNT(*), SUM(total_price) FROM [booking_db$(SUFFIX)].dbo.Booking_Detail GROUP BY item_type
UNION ALL SELECT 'Payment.amount', status, COUNT(*), SUM(amount) FROM [booking_db$(SUFFIX)].dbo.Payment GROUP BY status
UNION ALL SELECT 'Showtime_Seat.unit_price', status, COUNT(*), SUM(unit_price) FROM [booking_db$(SUFFIX)].dbo.Showtime_Seat GROUP BY status
ORDER BY item, state;
