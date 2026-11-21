-- Flyway executes this migration transactionally. Duplicate active titles or
-- invalid durations block migration; they are never silently merged or defaulted.
IF EXISTS (SELECT 1 FROM dbo.Movie GROUP BY UPPER(title) HAVING COUNT(*) > 1)
    THROW 51100, 'Duplicate legacy movie title. Reconcile titles before migration.', 1;
IF EXISTS (SELECT 1 FROM dbo.Movie WHERE duration <= 0)
    THROW 51101, 'Invalid legacy movie duration. Reconcile before migration.', 1;
GO

ALTER TABLE dbo.Movie DROP CONSTRAINT PK_Movie;
ALTER TABLE dbo.Movie ALTER COLUMN movie_id VARCHAR(36) COLLATE Latin1_General_100_BIN2 NOT NULL;
ALTER TABLE dbo.Movie ADD CONSTRAINT PK_Movie PRIMARY KEY (movie_id);
GO

-- The source has no inactive flag: existing movies start active.
ALTER TABLE dbo.Movie ADD active BIT NOT NULL CONSTRAINT DF_Movie_active DEFAULT (1) WITH VALUES;
ALTER TABLE dbo.Movie ADD version BIGINT NOT NULL CONSTRAINT DF_Movie_version DEFAULT (0) WITH VALUES;
GO
ALTER TABLE dbo.Movie ADD CONSTRAINT CK_Movie_duration CHECK (duration > 0);
ALTER TABLE dbo.Movie ADD CONSTRAINT CK_Movie_version CHECK (version >= 0);
-- A deactivated movie keeps its history but no longer reserves its title.
CREATE UNIQUE INDEX UX_Movie_active_title ON dbo.Movie(title) WHERE active = 1;
CREATE INDEX IX_Movie_active_release ON dbo.Movie(active,release_date,movie_id);
GO

-- Uploaded posters live with their owning domain so every Catalog instance can
-- serve them and a failed write cannot leave a stray file behind.
CREATE TABLE dbo.Movie_Image (
    image_id VARCHAR(36) COLLATE Latin1_General_100_BIN2 NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    data VARBINARY(MAX) NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT DF_Movie_Image_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_Movie_Image PRIMARY KEY (image_id),
    CONSTRAINT CK_Movie_Image_content_type CHECK (content_type IN ('image/jpeg','image/png','image/webp'))
);
