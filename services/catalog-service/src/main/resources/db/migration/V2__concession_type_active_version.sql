-- Flyway executes this migration transactionally. Unknown legacy categories
-- or invalid prices block migration; they are never silently defaulted.
IF EXISTS (SELECT 1 FROM dbo.Concession
           WHERE LEFT(concession_id,2) COLLATE Latin1_General_100_BIN2 NOT IN ('PC','DR'))
    THROW 51000, 'Unknown legacy concession type. Reconcile IDs before migration.', 1;
IF EXISTS (SELECT 1 FROM dbo.Concession WHERE price <= 0)
    THROW 51001, 'Invalid legacy concession price. Reconcile before migration.', 1;
GO

ALTER TABLE dbo.Concession DROP CONSTRAINT PK_Concession;
ALTER TABLE dbo.Concession ALTER COLUMN concession_id VARCHAR(36) COLLATE Latin1_General_100_BIN2 NOT NULL;
ALTER TABLE dbo.Concession ADD CONSTRAINT PK_Concession PRIMARY KEY (concession_id);
ALTER TABLE dbo.Concession ALTER COLUMN price DECIMAL(19,2) NOT NULL;
GO

ALTER TABLE dbo.Concession ADD type VARCHAR(2) NULL;
GO
UPDATE dbo.Concession SET type = LEFT(concession_id,2);
GO
ALTER TABLE dbo.Concession ALTER COLUMN type VARCHAR(2) NOT NULL;
-- The source has no inactive flag: existing catalog entries start active.
ALTER TABLE dbo.Concession ADD active BIT NOT NULL CONSTRAINT DF_Concession_active DEFAULT (1) WITH VALUES;
ALTER TABLE dbo.Concession ADD version BIGINT NOT NULL CONSTRAINT DF_Concession_version DEFAULT (0) WITH VALUES;
GO
ALTER TABLE dbo.Concession ADD CONSTRAINT CK_Concession_type CHECK (type COLLATE Latin1_General_100_BIN2 IN ('PC','DR'));
ALTER TABLE dbo.Concession ADD CONSTRAINT CK_Concession_price CHECK (price > 0);
ALTER TABLE dbo.Concession ADD CONSTRAINT CK_Concession_version CHECK (version >= 0);
CREATE INDEX IX_Concession_active_type ON dbo.Concession(active,type,concession_id);
