-- LOCAL/DEV. Restores the 4 backups from 01_backup_4db.sql as NEW copies (<db><suffix>, default *_restore) (B-05).
-- Never restores over the original databases: no REPLACE, and it stops if any target database already exists.
-- Data and log files get new names (<target>_<logical name>.mdf/.ldf), so they never clash with the originals'
-- files (the Msg 5170 seen during the cinema_db Flyway move).
-- Run with a login that has CREATE DATABASE (sysadmin or dbcreator). Not a service login.
SET NOCOUNT ON;
SET XACT_ABORT ON;

-- Settings: the same folder as the backup, the tag printed by 01, and the suffix of the copies.
DECLARE @dir NVARCHAR(260) = NULL;              -- NULL: instance default backup folder
DECLARE @tag NVARCHAR(20) = N'<backup tag>';    -- e.g. 20261107_093000
DECLARE @suffix SYSNAME = N'_restore';

IF @tag = N'<backup tag>' THROW 51511, 'Set @tag to the backup tag printed by 01_backup_4db.sql.', 1;
IF @suffix IS NULL OR LEN(@suffix) = 0 THROW 51512, 'A non-empty @suffix is required; this script never restores over the originals.', 1;
IF @dir IS NULL SET @dir = CONVERT(NVARCHAR(260), SERVERPROPERTY('InstanceDefaultBackupPath'));
DECLARE @sep NCHAR(1) = CASE WHEN CHARINDEX(N'/', @dir) > 0 THEN N'/' ELSE N'\' END;
IF RIGHT(@dir, 1) IN (N'/', N'\') SET @dir = LEFT(@dir, LEN(@dir) - 1);
DECLARE @dataDir NVARCHAR(260) = CONVERT(NVARCHAR(260), SERVERPROPERTY('InstanceDefaultDataPath'));
DECLARE @logDir NVARCHAR(260) = CONVERT(NVARCHAR(260), SERVERPROPERTY('InstanceDefaultLogPath'));
IF RIGHT(@dataDir, 1) NOT IN (N'/', N'\') SET @dataDir += @sep;
IF RIGHT(@logDir, 1) NOT IN (N'/', N'\') SET @logDir += @sep;

DECLARE @dbs TABLE (ord INT, source_db SYSNAME, target_db NVARCHAR(128) NULL, backup_file NVARCHAR(400) NULL);
INSERT @dbs (ord, source_db) VALUES (1, N'identity_db'), (2, N'catalog_db'), (3, N'cinema_db'), (4, N'booking_db');
UPDATE @dbs SET target_db = source_db + @suffix, backup_file = @dir + @sep + source_db + N'_' + @tag + N'.bak';

-- Check every target before restoring anything, so a run never stops half way because of an existing copy.
IF EXISTS (SELECT 1 FROM @dbs WHERE DB_ID(target_db) IS NOT NULL)
BEGIN
    SELECT target_db AS already_exists FROM @dbs WHERE DB_ID(target_db) IS NOT NULL;
    THROW 51513, 'A target copy already exists. Drop the old copy yourself or use another @suffix; nothing was restored.', 1;
END;

-- RESTORE FILELISTONLY columns of SQL Server 2016-2022.
CREATE TABLE #files (
    LogicalName NVARCHAR(128), PhysicalName NVARCHAR(260), [Type] CHAR(1), FileGroupName NVARCHAR(128),
    Size NUMERIC(20, 0), MaxSize NUMERIC(20, 0), FileId BIGINT, CreateLSN NUMERIC(25, 0), DropLSN NUMERIC(25, 0),
    UniqueId UNIQUEIDENTIFIER, ReadOnlyLSN NUMERIC(25, 0), ReadWriteLSN NUMERIC(25, 0), BackupSizeInBytes BIGINT,
    SourceBlockSize INT, FileGroupId INT, LogGroupGUID UNIQUEIDENTIFIER, DifferentialBaseLSN NUMERIC(25, 0),
    DifferentialBaseGUID UNIQUEIDENTIFIER, IsReadOnly BIT, IsPresent BIT, TDEThumbprint VARBINARY(32), SnapshotUrl NVARCHAR(360));

DECLARE @ord INT = 1, @src SYSNAME, @target SYSNAME, @file NVARCHAR(400), @move NVARCHAR(MAX), @sql NVARCHAR(MAX);
WHILE @ord <= 4
BEGIN
    SELECT @src = source_db, @target = target_db, @file = backup_file FROM @dbs WHERE ord = @ord;
    RESTORE VERIFYONLY FROM DISK = @file WITH CHECKSUM;

    TRUNCATE TABLE #files;
    SET @sql = N'RESTORE FILELISTONLY FROM DISK = N''' + REPLACE(@file, N'''', N'''''') + N'''';
    INSERT #files EXEC (@sql);

    SELECT @move = STRING_AGG(CONVERT(NVARCHAR(MAX),
        N'MOVE N''' + REPLACE(LogicalName, N'''', N'''''') + N''' TO N'''
        + REPLACE(CASE [Type] WHEN 'L' THEN @logDir ELSE @dataDir END + @target + N'_' + LogicalName
                  + CASE [Type] WHEN 'L' THEN N'.ldf' WHEN 'D' THEN CASE WHEN FileId = 1 THEN N'.mdf' ELSE N'.ndf' END ELSE N'' END,
                  N'''', N'''''') + N''''), N', ')
    FROM #files;

    SET @sql = N'RESTORE DATABASE ' + QUOTENAME(@target) + N' FROM DISK = N''' + REPLACE(@file, N'''', N'''''')
             + N''' WITH ' + @move + N', CHECKSUM, RECOVERY;';
    PRINT @sql;
    EXEC (@sql);
    SET @ord += 1;
END;

SELECT d.source_db, d.target_db, d.backup_file, DATABASEPROPERTYEX(d.target_db, 'Status') AS target_status
FROM @dbs d ORDER BY d.ord;
PRINT N'Done. Compare with 03_compare_copy.sql, then run 04_reconcile.sql with :setvar SUFFIX "' + @suffix + N'".';
