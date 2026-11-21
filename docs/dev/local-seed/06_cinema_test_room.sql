-- LOCAL/DEV ONLY. A separate test room for trying showtime create/update/Open Sales without occupying rooms 1-3.
-- Replaces docs/migration/cinema_local_seed.sql, whose room R1 and seat IDs TS000001-TS000008 clashed with real data.
-- Needs cinema_db created by Flyway (cinema-service V1). Safe to re-run: existing rows are kept, never updated.
-- IDs are reserved for tests only: room 901, template TPTEST01, seats TPTEST01-A1 ... TPTEST01-B4.
USE [cinema_db];
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.flyway_schema_history', N'U') IS NULL OR OBJECT_ID(N'dbo.Room', N'U') IS NULL
    THROW 51401, 'cinema_db has no Flyway schema. Start cinema-service once against cinema_db first.', 1;
IF EXISTS (SELECT 1 FROM dbo.Room WHERE room_id = 901 AND template_id <> 'TPTEST01')
    THROW 51402, 'Room 901 exists with another template; reconcile manually.', 1;

BEGIN TRAN;
IF NOT EXISTS (SELECT 1 FROM dbo.Template WHERE template_id = 'TPTEST01')
    INSERT dbo.Template (template_id, name, description) VALUES ('TPTEST01', 'TEST', 'Test room only, not a real screen');

INSERT dbo.Template_Seat (template_seat_id, template_id, row_label, seat_number, seat_type)
SELECT v.id, 'TPTEST01', v.row_label, v.seat_number, v.seat_type
FROM (VALUES
    ('TPTEST01-A1', 'A', 1, 'STANDARD'), ('TPTEST01-A2', 'A', 2, 'STANDARD'),
    ('TPTEST01-A3', 'A', 3, 'STANDARD'), ('TPTEST01-A4', 'A', 4, 'STANDARD'),
    ('TPTEST01-B1', 'B', 1, 'VIP'), ('TPTEST01-B2', 'B', 2, 'VIP'),
    ('TPTEST01-B3', 'B', 3, 'VIP'), ('TPTEST01-B4', 'B', 4, 'VIP')
) AS v (id, row_label, seat_number, seat_type)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Template_Seat s WHERE s.template_seat_id = v.id);

IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE room_id = 901)
BEGIN
    SET IDENTITY_INSERT dbo.Room ON;
    INSERT dbo.Room (room_id, template_id, status) VALUES (901, 'TPTEST01', 'ACTIVE');
    SET IDENTITY_INSERT dbo.Room OFF;
    -- An explicit 901 would make the next automatic room ID 902; keep numbering after the real rooms instead.
    DECLARE @lastRealRoom INT = (SELECT ISNULL(MAX(room_id), 0) FROM dbo.Room WHERE room_id < 900);
    DBCC CHECKIDENT ('dbo.Room', RESEED, @lastRealRoom) WITH NO_INFOMSGS;
END;
COMMIT;

SELECT r.room_id, r.template_id, r.status, COUNT(s.template_seat_id) AS seats
FROM dbo.Room r LEFT JOIN dbo.Template_Seat s ON s.template_id = r.template_id
GROUP BY r.room_id, r.template_id, r.status ORDER BY r.room_id;
