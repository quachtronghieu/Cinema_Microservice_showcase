-- LOCAL/DEV ONLY. Copies the old Hibernate-made Cinema database into a new cinema_db created by Flyway V1/V2.
-- Before running: back up and rename the old database to cinema_db_before_flyway, create an empty cinema_db,
-- start cinema-service once so Flyway creates the tables, then stop it. See README section "Chuyển cinema_db sang Flyway".
-- Reads cinema_db_before_flyway only; never changes or drops it. Rooms whose ID is not a number (for example R1 of
-- the earlier test seed) and everything that only they use are not copied; the last result set lists them.
USE [cinema_db];
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

IF DB_ID(N'cinema_db_before_flyway') IS NULL
    THROW 51301, 'Missing cinema_db_before_flyway. Rename the old cinema_db first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.flyway_schema_history WHERE version = '2' AND success = 1)
    THROW 51302, 'cinema_db has no Flyway V2. Start cinema-service once against the new empty cinema_db.', 1;
IF EXISTS (SELECT 1 FROM dbo.Room) OR EXISTS (SELECT 1 FROM dbo.Template) OR EXISTS (SELECT 1 FROM dbo.Showtime)
    THROW 51303, 'cinema_db already has data; this script only fills a new empty database.', 1;

-- Deferred compilation: the preflight messages above appear even when the old tables are missing.
BEGIN TRY
    EXEC sys.sp_executesql N'
BEGIN TRAN;

-- Rooms keep their numeric IDs; status is normalized (Active -> ACTIVE). Any other value fails the CHECK and rolls back.
SELECT CONVERT(int, r.room_id) AS room_id, r.template_id, UPPER(LTRIM(RTRIM(r.status))) AS status
INTO #rooms
FROM cinema_db_before_flyway.dbo.Room r
WHERE TRY_CONVERT(int, r.room_id) IS NOT NULL;

INSERT dbo.Template (template_id, name, description)
SELECT t.template_id, t.name, t.description
FROM cinema_db_before_flyway.dbo.Template t
WHERE t.template_id IN (SELECT template_id FROM #rooms);

INSERT dbo.Template_Seat (template_seat_id, template_id, row_label, seat_number, seat_type)
SELECT s.template_seat_id, s.template_id, s.row_label, s.seat_number, UPPER(s.seat_type)
FROM cinema_db_before_flyway.dbo.Template_Seat s
WHERE s.template_id IN (SELECT template_id FROM dbo.Template);

SET IDENTITY_INSERT dbo.Room ON;
INSERT dbo.Room (room_id, template_id, status) SELECT room_id, template_id, status FROM #rooms;
SET IDENTITY_INSERT dbo.Room OFF;

-- A missing sales status was read as OPEN by the old code, so it is copied as OPEN.
INSERT dbo.Showtime (showtime_id, movie_id, room_id, show_date, start_time, end_time, sales_status)
SELECT s.showtime_id, s.movie_id, CONVERT(int, s.room_id), s.show_date, s.start_time, s.end_time,
       ISNULL(UPPER(s.sales_status), ''OPEN'')
FROM cinema_db_before_flyway.dbo.Showtime s
WHERE CONVERT(varchar(36), s.room_id) IN (SELECT CONVERT(varchar(36), room_id) FROM #rooms);

INSERT dbo.Showtime_Seat (showtime_seat_id, showtime_id, template_seat_id, status)
SELECT ss.showtime_seat_id, ss.showtime_id, ss.template_seat_id, ss.status
FROM cinema_db_before_flyway.dbo.Showtime_Seat ss
WHERE ss.showtime_id IN (SELECT showtime_id FROM dbo.Showtime)
  AND ss.template_seat_id IN (SELECT template_seat_id FROM dbo.Template_Seat);

COMMIT;

SELECT ''Template'' AS table_name, (SELECT COUNT(*) FROM cinema_db_before_flyway.dbo.Template) AS old_rows, (SELECT COUNT(*) FROM dbo.Template) AS new_rows
UNION ALL SELECT ''Template_Seat'', (SELECT COUNT(*) FROM cinema_db_before_flyway.dbo.Template_Seat), (SELECT COUNT(*) FROM dbo.Template_Seat)
UNION ALL SELECT ''Room'', (SELECT COUNT(*) FROM cinema_db_before_flyway.dbo.Room), (SELECT COUNT(*) FROM dbo.Room)
UNION ALL SELECT ''Showtime'', (SELECT COUNT(*) FROM cinema_db_before_flyway.dbo.Showtime), (SELECT COUNT(*) FROM dbo.Showtime)
UNION ALL SELECT ''Showtime_Seat'', (SELECT COUNT(*) FROM cinema_db_before_flyway.dbo.Showtime_Seat), (SELECT COUNT(*) FROM dbo.Showtime_Seat);

SELECT r.room_id, r.template_id, r.status, COUNT(s.template_seat_id) AS seats
FROM dbo.Room r LEFT JOIN dbo.Template_Seat s ON s.template_id = r.template_id
GROUP BY r.room_id, r.template_id, r.status ORDER BY r.room_id;

-- Not copied: rooms with a non-numeric ID and their showtimes.
SELECT ''Room'' AS skipped, CONVERT(varchar(36), room_id) AS id FROM cinema_db_before_flyway.dbo.Room WHERE TRY_CONVERT(int, room_id) IS NULL
UNION ALL
SELECT ''Showtime'', showtime_id FROM cinema_db_before_flyway.dbo.Showtime WHERE TRY_CONVERT(int, room_id) IS NULL;';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRAN;
    THROW;
END CATCH;
