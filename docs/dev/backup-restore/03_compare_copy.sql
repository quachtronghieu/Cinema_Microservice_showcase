-- LOCAL/DEV. Read only. Compares every table of the 4 databases with its restored copy (B-05/B-06):
-- row count and CHECKSUM_AGG(BINARY_CHECKSUM(*)) per table, plus tables found on one side only.
-- Every row should have match = 1 when nothing wrote to the originals after the backup.
-- The checksum is a quick fingerprint, not proof of equality: two different tables can, rarely, give the same value.
SET NOCOUNT ON;

DECLARE @suffix SYSNAME = N'_restore';

DECLARE @dbs TABLE (ord INT, source_db SYSNAME);
INSERT @dbs VALUES (1, N'identity_db'), (2, N'catalog_db'), (3, N'cinema_db'), (4, N'booking_db');
IF EXISTS (SELECT 1 FROM @dbs WHERE DB_ID(source_db) IS NULL OR DB_ID(source_db + @suffix) IS NULL)
BEGIN
    SELECT source_db, source_db + @suffix AS copy_db, DB_ID(source_db) AS source_id, DB_ID(source_db + @suffix) AS copy_id
    FROM @dbs WHERE DB_ID(source_db) IS NULL OR DB_ID(source_db + @suffix) IS NULL;
    THROW 51521, 'A database or its copy is missing.', 1;
END;

CREATE TABLE #tables (source_db SYSNAME, side VARCHAR(6), table_name NVARCHAR(300));
CREATE TABLE #cmp (source_db SYSNAME, table_name NVARCHAR(300),
                   source_rows BIGINT NULL, copy_rows BIGINT NULL, source_checksum INT NULL, copy_checksum INT NULL);

DECLARE @ord INT = 1, @db SYSNAME, @copy SYSNAME, @sql NVARCHAR(MAX);
WHILE @ord <= 4
BEGIN
    SELECT @db = source_db, @copy = source_db + @suffix FROM @dbs WHERE ord = @ord;
    SET @sql = N'SELECT @db, ''source'', QUOTENAME(s.name) + ''.'' + QUOTENAME(t.name) FROM ' + QUOTENAME(@db)
             + N'.sys.tables t JOIN ' + QUOTENAME(@db) + N'.sys.schemas s ON s.schema_id = t.schema_id WHERE t.is_ms_shipped = 0;';
    INSERT #tables EXEC sys.sp_executesql @sql, N'@db SYSNAME', @db = @db;
    SET @sql = N'SELECT @db, ''copy'', QUOTENAME(s.name) + ''.'' + QUOTENAME(t.name) FROM ' + QUOTENAME(@copy)
             + N'.sys.tables t JOIN ' + QUOTENAME(@copy) + N'.sys.schemas s ON s.schema_id = t.schema_id WHERE t.is_ms_shipped = 0;';
    INSERT #tables EXEC sys.sp_executesql @sql, N'@db SYSNAME', @db = @db;
    SET @ord += 1;
END;

INSERT #cmp (source_db, table_name)
SELECT DISTINCT source_db, table_name FROM #tables;

DECLARE @t NVARCHAR(300), @rows BIGINT, @sum INT, @hasSource BIT, @hasCopy BIT;
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT source_db, table_name FROM #cmp;
OPEN c;
FETCH NEXT FROM c INTO @db, @t;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @copy = @db + @suffix;
    SET @hasSource = CASE WHEN EXISTS (SELECT 1 FROM #tables WHERE source_db = @db AND table_name = @t AND side = 'source') THEN 1 ELSE 0 END;
    SET @hasCopy = CASE WHEN EXISTS (SELECT 1 FROM #tables WHERE source_db = @db AND table_name = @t AND side = 'copy') THEN 1 ELSE 0 END;
    IF @hasSource = 1
    BEGIN
        SET @sql = N'SELECT @r = COUNT_BIG(*), @s = CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM ' + QUOTENAME(@db) + N'.' + @t + N';';
        EXEC sys.sp_executesql @sql, N'@r BIGINT OUTPUT, @s INT OUTPUT', @r = @rows OUTPUT, @s = @sum OUTPUT;
        UPDATE #cmp SET source_rows = @rows, source_checksum = @sum WHERE source_db = @db AND table_name = @t;
    END;
    IF @hasCopy = 1
    BEGIN
        SET @sql = N'SELECT @r = COUNT_BIG(*), @s = CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM ' + QUOTENAME(@copy) + N'.' + @t + N';';
        EXEC sys.sp_executesql @sql, N'@r BIGINT OUTPUT, @s INT OUTPUT', @r = @rows OUTPUT, @s = @sum OUTPUT;
        UPDATE #cmp SET copy_rows = @rows, copy_checksum = @sum WHERE source_db = @db AND table_name = @t;
    END;
    FETCH NEXT FROM c INTO @db, @t;
END;
CLOSE c;
DEALLOCATE c;

SELECT source_db, table_name, source_rows, copy_rows, source_checksum, copy_checksum,
       CASE WHEN source_rows IS NOT NULL AND copy_rows IS NOT NULL AND source_rows = copy_rows
                 AND ISNULL(source_checksum, 0) = ISNULL(copy_checksum, 0) THEN 1 ELSE 0 END AS match
FROM #cmp ORDER BY source_db, table_name;

SELECT COUNT(*) AS tables_compared,
       SUM(CASE WHEN source_rows = copy_rows AND ISNULL(source_checksum, 0) = ISNULL(copy_checksum, 0) THEN 0 ELSE 1 END) AS mismatches
FROM #cmp;
