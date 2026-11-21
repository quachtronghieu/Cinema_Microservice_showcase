-- Local/dev import of legacy movies into catalog_db (run after catalog-service applied Flyway through V4).
-- Source: Cinema_Management1.dbo.Movie on the same SQL Server instance. Safe to re-run: existing IDs are skipped.
-- Keeps legacy IDs, titles, dates and image paths unchanged (docs/migration.md "Movie migration").
-- Rows whose title already belongs to an active movie stop the import through UX_Movie_active_title; reconcile them first.
SET NOCOUNT ON;
SET XACT_ABORT ON;
-- sqlcmd defaults this to OFF, which SQL Server rejects on a table with a filtered index.
SET QUOTED_IDENTIFIER ON;
BEGIN TRAN;

INSERT INTO catalog_db.dbo.Movie (movie_id, title, duration, genre, release_date, summary, age_rating, img, trailer, active, version)
SELECT o.movie_id, o.title, o.duration, o.genre, o.release_date, o.summary, o.age_rating, o.img, o.trailer, 1, 0
FROM Cinema_Management1.dbo.Movie o
-- catalog_db uses a BIN2 collation for IDs, the legacy DB uses CI_AS.
WHERE NOT EXISTS (SELECT 1 FROM catalog_db.dbo.Movie m
                  WHERE m.movie_id = o.movie_id COLLATE Latin1_General_100_BIN2);

SELECT @@ROWCOUNT AS imported;
COMMIT;

SELECT COUNT(*) AS movies, MIN(release_date) AS earliest, MAX(release_date) AS latest FROM catalog_db.dbo.Movie;
-- Posters referenced by imported rows must exist among the files bundled with Catalog.
SELECT movie_id, title, img FROM catalog_db.dbo.Movie
WHERE img NOT LIKE '/assets/img/movies/%' ORDER BY movie_id;
