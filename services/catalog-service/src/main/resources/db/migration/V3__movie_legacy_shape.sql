-- Target: catalog_db only. For a staged import, migrate to V3, import the
-- legacy Movie rows preserving IDs, then apply V4. See docs/migration.md.
CREATE TABLE dbo.Movie (
    movie_id VARCHAR(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
    title VARCHAR(200) NOT NULL,
    duration INT NOT NULL,
    genre VARCHAR(100) NOT NULL,
    release_date DATE NOT NULL,
    summary VARCHAR(MAX) NULL,
    age_rating VARCHAR(10) NULL,
    img VARCHAR(255) NOT NULL,
    trailer VARCHAR(255) NOT NULL,
    CONSTRAINT PK_Movie PRIMARY KEY (movie_id)
);
