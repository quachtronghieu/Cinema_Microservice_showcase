-- LOCAL/DEV. Full COPY_ONLY backup of the 4 service databases (B-05, docs/ngan-task-scope.md).
-- Reads the databases only: no row is changed. Writes 4 .bak files and a backup history row in msdb.
-- COPY_ONLY leaves any existing backup chain untouched.
-- Run with a login that can back up all 4 DBs (sysadmin or db_backupoperator in each). Not a service login.
-- Stop the 4 services (or at least all writes) first, so the 4 files describe the same moment and
-- cross-database references (booking -> cinema -> catalog -> identity) still match after a restore.
SET NOCOUNT ON;
SET XACT_ABORT ON;

-- Settings. @dir NULL uses the instance default backup folder (Docker/Linux: usually /var/opt/mssql/data).
-- The folder is seen by the SQL Server process, not by SSMS: in Docker it is inside the container.
DECLARE @dir NVARCHAR(260) = NULL;
DECLARE @tag NVARCHAR(20) = FORMAT(SYSUTCDATETIME(), 'yyyyMMdd_HHmmss');  -- UTC time, part of each file name

IF @dir IS NULL SET @dir = CONVERT(NVARCHAR(260), SERVERPROPERTY('InstanceDefaultBackupPath'));
DECLARE @sep NCHAR(1) = CASE WHEN CHARINDEX(N'/', @dir) > 0 THEN N'/' ELSE N'\' END;
IF RIGHT(@dir, 1) IN (N'/', N'\') SET @dir = LEFT(@dir, LEN(@dir) - 1);

DECLARE @dbs TABLE (ord INT, name SYSNAME);
INSERT @dbs VALUES (1, N'identity_db'), (2, N'catalog_db'), (3, N'cinema_db'), (4, N'booking_db');

IF EXISTS (SELECT 1 FROM @dbs d WHERE DB_ID(d.name) IS NULL)
BEGIN
    SELECT d.name AS missing_database FROM @dbs d WHERE DB_ID(d.name) IS NULL;
    THROW 51501, 'One of the 4 databases does not exist; nothing was backed up.', 1;
END;

DECLARE @result TABLE (database_name SYSNAME, backup_file NVARCHAR(400), verified BIT);
DECLARE @name SYSNAME, @file NVARCHAR(400), @sql NVARCHAR(MAX), @ord INT = 1;
WHILE @ord <= 4
BEGIN
    SELECT @name = name FROM @dbs WHERE ord = @ord;
    SET @file = @dir + @sep + @name + N'_' + @tag + N'.bak';
    -- CHECKSUM makes SQL Server check every page while writing; INIT overwrites only a file with this exact name.
    SET @sql = N'BACKUP DATABASE ' + QUOTENAME(@name) + N' TO DISK = @f WITH COPY_ONLY, INIT, CHECKSUM, NAME = @n;';
    EXEC sys.sp_executesql @sql, N'@f NVARCHAR(400), @n NVARCHAR(128)', @f = @file, @n = @name;
    RESTORE VERIFYONLY FROM DISK = @file WITH CHECKSUM;
    INSERT @result VALUES (@name, @file, 1);
    SET @ord += 1;
END;

SELECT @tag AS backup_tag, database_name, backup_file, verified FROM @result;
PRINT N'Backup tag: ' + @tag + N'. Use it in 02_restore_as_copy.sql.';
