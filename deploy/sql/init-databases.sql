-- Provisions the 4 microservice databases and their separate migration/runtime logins (AGENTS 3, 13).
-- Safe to re-run. Passwords arrive as sqlcmd variables and are never stored in Git.
IF DB_ID('identity_db') IS NULL CREATE DATABASE identity_db;
IF DB_ID('catalog_db') IS NULL CREATE DATABASE catalog_db;
IF DB_ID('cinema_db') IS NULL CREATE DATABASE cinema_db;
IF DB_ID('booking_db') IS NULL CREATE DATABASE booking_db;
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
IF USER_ID('identity_migrator') IS NULL CREATE USER identity_migrator FOR LOGIN identity_migrator;
IF USER_ID('identity_app') IS NULL CREATE USER identity_app FOR LOGIN identity_app;
ALTER ROLE db_owner ADD MEMBER identity_migrator;
ALTER ROLE db_datareader ADD MEMBER identity_app;
ALTER ROLE db_datawriter ADD MEMBER identity_app;
GO

USE catalog_db;
IF USER_ID('catalog_migrator') IS NULL CREATE USER catalog_migrator FOR LOGIN catalog_migrator;
IF USER_ID('catalog_app') IS NULL CREATE USER catalog_app FOR LOGIN catalog_app;
ALTER ROLE db_owner ADD MEMBER catalog_migrator;
ALTER ROLE db_datareader ADD MEMBER catalog_app;
ALTER ROLE db_datawriter ADD MEMBER catalog_app;
GO

USE cinema_db;
IF USER_ID('cinema_migrator') IS NULL CREATE USER cinema_migrator FOR LOGIN cinema_migrator;
IF USER_ID('cinema_app') IS NULL CREATE USER cinema_app FOR LOGIN cinema_app;
ALTER ROLE db_owner ADD MEMBER cinema_migrator;
ALTER ROLE db_datareader ADD MEMBER cinema_app;
ALTER ROLE db_datawriter ADD MEMBER cinema_app;
GO

USE booking_db;
IF USER_ID('booking_migrator') IS NULL CREATE USER booking_migrator FOR LOGIN booking_migrator;
IF USER_ID('booking_app') IS NULL CREATE USER booking_app FOR LOGIN booking_app;
ALTER ROLE db_owner ADD MEMBER booking_migrator;
ALTER ROLE db_datareader ADD MEMBER booking_app;
ALTER ROLE db_datawriter ADD MEMBER booking_app;
GO
