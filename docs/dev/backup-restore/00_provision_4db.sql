-- LOCAL/DEV. Creates the 4 empty service databases and their migrator/runtime logins, for a rebuild from scratch (task 7).
-- Needs SQLCMD mode and a sysadmin login. Passwords are sqlcmd variables and are never stored in Git, for example:
--   sqlcmd -I -i 00_provision_4db.sql -v IDENTITY_MIGRATION_PASSWORD="..." IDENTITY_DB_PASSWORD="..." CATALOG_MIGRATION_PASSWORD="..."
--          CATALOG_DB_PASSWORD="..." CINEMA_MIGRATION_PASSWORD="..." CINEMA_DB_PASSWORD="..." BOOKING_MIGRATION_PASSWORD="..." BOOKING_DB_PASSWORD="..."
-- Meant for an empty SQL Server. It stops if one of the 4 databases already exists, so it cannot touch seeded data.
-- Migrator: db_owner of its own database, used only by Flyway. Runtime: read/write only (AGENTS sections 3, 22).
:on error exit
SET NOCOUNT ON;
IF DB_ID(N'identity_db') IS NOT NULL OR DB_ID(N'catalog_db') IS NOT NULL OR DB_ID(N'cinema_db') IS NOT NULL OR DB_ID(N'booking_db') IS NOT NULL
    THROW 51541, 'One of the 4 databases already exists. This script is only for an empty SQL Server; nothing was changed.', 1;
GO
CREATE DATABASE identity_db;
CREATE DATABASE catalog_db;
CREATE DATABASE cinema_db;
CREATE DATABASE booking_db;
GO
IF SUSER_ID('identity_migrator') IS NULL CREATE LOGIN identity_migrator WITH PASSWORD = '$(IDENTITY_MIGRATION_PASSWORD)';
IF SUSER_ID('identity_app') IS NULL CREATE LOGIN identity_app WITH PASSWORD = '$(IDENTITY_DB_PASSWORD)';
IF SUSER_ID('catalog_migrator') IS NULL CREATE LOGIN catalog_migrator WITH PASSWORD = '$(CATALOG_MIGRATION_PASSWORD)';
IF SUSER_ID('catalog_app') IS NULL CREATE LOGIN catalog_app WITH PASSWORD = '$(CATALOG_DB_PASSWORD)';
IF SUSER_ID('cinema_migrator') IS NULL CREATE LOGIN cinema_migrator WITH PASSWORD = '$(CINEMA_MIGRATION_PASSWORD)';
IF SUSER_ID('cinema_app') IS NULL CREATE LOGIN cinema_app WITH PASSWORD = '$(CINEMA_DB_PASSWORD)';
IF SUSER_ID('booking_migrator') IS NULL CREATE LOGIN booking_migrator WITH PASSWORD = '$(BOOKING_MIGRATION_PASSWORD)';
IF SUSER_ID('booking_app') IS NULL CREATE LOGIN booking_app WITH PASSWORD = '$(BOOKING_DB_PASSWORD)';
GO
USE identity_db;
CREATE USER identity_migrator FOR LOGIN identity_migrator; ALTER ROLE db_owner ADD MEMBER identity_migrator;
CREATE USER identity_app FOR LOGIN identity_app; ALTER ROLE db_datareader ADD MEMBER identity_app; ALTER ROLE db_datawriter ADD MEMBER identity_app;
GO
USE catalog_db;
CREATE USER catalog_migrator FOR LOGIN catalog_migrator; ALTER ROLE db_owner ADD MEMBER catalog_migrator;
CREATE USER catalog_app FOR LOGIN catalog_app; ALTER ROLE db_datareader ADD MEMBER catalog_app; ALTER ROLE db_datawriter ADD MEMBER catalog_app;
GO
USE cinema_db;
CREATE USER cinema_migrator FOR LOGIN cinema_migrator; ALTER ROLE db_owner ADD MEMBER cinema_migrator;
CREATE USER cinema_app FOR LOGIN cinema_app; ALTER ROLE db_datareader ADD MEMBER cinema_app; ALTER ROLE db_datawriter ADD MEMBER cinema_app;
GO
USE booking_db;
CREATE USER booking_migrator FOR LOGIN booking_migrator; ALTER ROLE db_owner ADD MEMBER booking_migrator;
CREATE USER booking_app FOR LOGIN booking_app; ALTER ROLE db_datareader ADD MEMBER booking_app; ALTER ROLE db_datawriter ADD MEMBER booking_app;
GO
SELECT d.name AS database_name, d.state_desc FROM sys.databases d
WHERE d.name IN (N'identity_db', N'catalog_db', N'cinema_db', N'booking_db') ORDER BY d.name;
