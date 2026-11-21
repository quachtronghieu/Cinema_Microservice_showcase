-- Target: catalog_db only. For a staged import, migrate to V1, import the
-- legacy Concession rows preserving IDs, then apply V2. See docs/migration.md.
CREATE TABLE dbo.Concession (
    concession_id VARCHAR(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    img VARCHAR(255) NOT NULL,
    CONSTRAINT PK_Concession PRIMARY KEY (concession_id)
);
