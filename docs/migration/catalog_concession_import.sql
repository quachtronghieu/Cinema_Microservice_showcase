-- Local/dev import of legacy concessions into catalog_db (run after catalog-service applied Flyway V1+V2).
-- Source: Cinema_Management1.dbo.Concession on the same SQL Server instance. Safe to re-run: existing IDs are skipped.
-- Keeps legacy IDs, names, descriptions, prices and image paths unchanged (docs/migration.md "Controlled legacy import").
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRAN;

INSERT INTO catalog_db.dbo.Concession (concession_id, name, price, description, img, type, active, version)
SELECT o.concession_id, o.name, o.price, o.description, o.img,
       LEFT(o.concession_id, 2) AS type,
       -- PC000003 'ABC' (12,000,000 VND) is leftover test data in the legacy DB; import it hidden.
       CASE WHEN o.concession_id = 'PC000003' THEN 0 ELSE 1 END AS active,
       0 AS version
FROM Cinema_Management1.dbo.Concession o
WHERE LEFT(o.concession_id, 2) IN ('PC', 'DR')
  -- catalog_db uses a BIN2 collation, the legacy DB uses CI_AS.
  AND NOT EXISTS (SELECT 1 FROM catalog_db.dbo.Concession c
                  WHERE c.concession_id = o.concession_id COLLATE Latin1_General_100_BIN2);

SELECT @@ROWCOUNT AS imported;
COMMIT;

SELECT concession_id, type, name, price, active FROM catalog_db.dbo.Concession ORDER BY type, concession_id;
