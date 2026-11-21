USE [booking_db];
-- LOCAL/DEV ONLY. Run against the named database after its schema exists.
-- Insert only: existing rows are not updated or deleted. Stop application writes while seeding.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

-- Dev fixture only, not proof of the Cinema -> Booking publish workflow.
-- Requires all 3 DBs on the same SQL Server; cross-DB reads are ETL only.
-- Do not create tables here: Flyway owns Booking schema/history.
IF OBJECT_ID(N'dbo.Sales_Snapshot', N'U') IS NULL
    THROW 51210, 'Missing booking_db.dbo.Sales_Snapshot. Run booking-service Flyway V1 against booking_db before this seed.', 1;
IF OBJECT_ID(N'dbo.Showtime_Seat', N'U') IS NULL
    THROW 51211, 'Missing booking_db.dbo.Showtime_Seat. Apply Booking migrations before this seed.', 1;
IF OBJECT_ID(N'cinema_db.dbo.Showtime', N'U') IS NULL
    THROW 51212, 'Missing cinema_db.dbo.Showtime. Prepare Cinema schema and run 02_cinema.sql first.', 1;
IF OBJECT_ID(N'catalog_db.dbo.Movie', N'U') IS NULL
    THROW 51213, 'Missing catalog_db.dbo.Movie. Prepare Catalog schema and run 01_catalog.sql first.', 1;
-- Deferred compilation ensures missing tables produce the preflight message above.
BEGIN TRY
    EXEC sys.sp_executesql N'BEGIN TRAN;
IF (SELECT COUNT(*) FROM cinema_db.dbo.Showtime WHERE showtime_id IN (''SW00023'',''SW00024'') AND movie_id=''MV000016'')<>2 THROW 51204, ''Run 01 and 02 first; missing demo showtimes.'', 1;
-- Reject invalid target rooms; unrelated legacy room R1 is never mapped to room 1.
IF EXISTS (SELECT 1 FROM cinema_db.dbo.Showtime WHERE showtime_id IN (''SW00023'',''SW00024'') AND (TRY_CONVERT(int,room_id) IS NULL OR CONVERT(varchar(36),room_id) NOT IN (''1'',''2'')))
    THROW 51214, ''Demo showtimes must reference numeric room IDs 1 or 2. Do not rename R1 automatically.'', 1;
INSERT dbo.Sales_Snapshot(showtime_id,snapshot_version,sales_status,movie_id,movie_title,room_id,room_name,show_date,start_time,end_time)
SELECT s.showtime_id,1,''OPEN'',s.movie_id,m.title,TRY_CONVERT(int,s.room_id),CONCAT(''Room '',s.room_id),s.show_date,s.start_time,s.end_time
FROM cinema_db.dbo.Showtime s JOIN catalog_db.dbo.Movie m ON m.movie_id=s.movie_id COLLATE Latin1_General_100_BIN2
WHERE s.showtime_id IN (''SW00023'',''SW00024'') AND NOT EXISTS (SELECT 1 FROM dbo.Sales_Snapshot x WHERE x.showtime_id=s.showtime_id COLLATE Latin1_General_100_BIN2);
INSERT dbo.Showtime_Seat(showtime_seat_id,showtime_id,template_seat_id,row_label,seat_number,seat_type,unit_price,status)
SELECT CONCAT(''HS'',RIGHT(''000000''+CONVERT(varchar(6),200000+CONVERT(int,RIGHT(t.template_seat_id,6))),6)),x.showtime_id,t.template_seat_id,t.row_label,t.seat_number,UPPER(t.seat_type),CASE UPPER(t.seat_type) WHEN ''VIP'' THEN 100000.00 ELSE 80000.00 END,''AVAILABLE''
FROM dbo.Sales_Snapshot x JOIN cinema_db.dbo.Room r ON TRY_CONVERT(int,r.room_id)=x.room_id AND CONVERT(varchar(36),r.room_id)=CONVERT(varchar(36),x.room_id)
JOIN cinema_db.dbo.Template_Seat t ON t.template_id=r.template_id
WHERE x.showtime_id IN (''SW00023'',''SW00024'') AND NOT EXISTS (SELECT 1 FROM dbo.Showtime_Seat y WHERE y.showtime_id=x.showtime_id AND y.template_seat_id=t.template_seat_id COLLATE Latin1_General_100_BIN2);
COMMIT;
SELECT showtime_id,status,COUNT(*) AS seats FROM dbo.Showtime_Seat WHERE showtime_id IN (''SW00023'',''SW00024'') GROUP BY showtime_id,status;
SELECT showtime_seat_id,showtime_id,row_label,seat_number,status FROM dbo.Showtime_Seat WHERE showtime_id IN (''SW00023'',''SW00024'') ORDER BY showtime_id,row_label,seat_number;';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRAN;
    THROW;
END CATCH;
