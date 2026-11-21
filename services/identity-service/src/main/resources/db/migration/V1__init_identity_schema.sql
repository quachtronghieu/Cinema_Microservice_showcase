-- =========================================================================
-- V1__init_identity_schema.sql
-- Database: identity_db
-- Owner: identity-service (In-charge: Nguyen Phuoc Thinh - CE190087)
-- References: AGENTS_1.3.md Sections 3, 7, 19, 24, 25
-- =========================================================================

-- 1. Table: Staff (Legacy staff data structure extended to VARCHAR(36))
IF OBJECT_ID('dbo.Staff', 'U') IS NULL
BEGIN
    CREATE TABLE Staff (
        staff_id VARCHAR(36) NOT NULL,
        full_name NVARCHAR(100) NOT NULL,
        position VARCHAR(50) NOT NULL,
        username VARCHAR(100) NOT NULL,
        password_hash VARCHAR(255) NULL,
        phone VARCHAR(15) NOT NULL,
        email VARCHAR(100) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_Staff_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_Staff_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Staff PRIMARY KEY (staff_id),
        CONSTRAINT UQ_Staff_username UNIQUE (username)
    );
END;

-- 2. Table: Customer (Legacy customer data structure extended to VARCHAR(36))
IF OBJECT_ID('dbo.Customer', 'U') IS NULL
BEGIN
    CREATE TABLE Customer (
        user_id VARCHAR(36) NOT NULL,
        username VARCHAR(100) NOT NULL,
        password_hash VARCHAR(255) NULL,
        dob DATE NOT NULL,
        sex BIT NOT NULL,
        email VARCHAR(100) NOT NULL,
        phone VARCHAR(15) NOT NULL,
        verify VARCHAR(255) NULL,
        reset_requested_at DATETIME2(3) NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_Customer_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_Customer_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Customer PRIMARY KEY (user_id),
        CONSTRAINT UQ_Customer_username UNIQUE (username)
    );
END;

-- 3. Table: Shift_Schedule (Workforce Module in identity-service)
IF OBJECT_ID('dbo.Shift_Schedule', 'U') IS NULL
BEGIN
    CREATE TABLE Shift_Schedule (
        shift_schedule_id VARCHAR(36) NOT NULL,
        staff_id VARCHAR(36) NOT NULL,
        shift_date DATE NOT NULL,
        status VARCHAR(20) NULL,
        shift_start TIME(7) NOT NULL,
        late_minute INT NULL DEFAULT 0,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_ShiftSchedule_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_ShiftSchedule_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Shift_Schedule PRIMARY KEY (shift_schedule_id),
        CONSTRAINT FK_ShiftSchedule_Staff FOREIGN KEY (staff_id) REFERENCES Staff(staff_id) ON DELETE CASCADE
    );
END;

-- 4. Table: Principal (Unified authentication entity for both Customer and Staff)
IF OBJECT_ID('dbo.Principal', 'U') IS NULL
BEGIN
    CREATE TABLE Principal (
        principal_id VARCHAR(36) NOT NULL,
        username VARCHAR(100) NOT NULL,
        normalized_username VARCHAR(100) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        account_type VARCHAR(20) NOT NULL,
        customer_id VARCHAR(36) NULL,
        staff_id VARCHAR(36) NULL,
        is_active BIT NOT NULL CONSTRAINT DF_Principal_is_active DEFAULT 1,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_Principal_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_Principal_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Principal PRIMARY KEY (principal_id),
        CONSTRAINT UQ_Principal_normalized_username UNIQUE (normalized_username),
        CONSTRAINT CK_Principal_account_type CHECK (account_type IN ('CUSTOMER', 'STAFF')),
        CONSTRAINT FK_Principal_Customer FOREIGN KEY (customer_id) REFERENCES Customer(user_id) ON DELETE SET NULL,
        CONSTRAINT FK_Principal_Staff FOREIGN KEY (staff_id) REFERENCES Staff(staff_id) ON DELETE SET NULL
    );
END;

-- 5. Table: Principal_Role (Role-based access control mapping)
-- Allowed Roles according to AGENTS 1.3: USER, CASHIER_STAFF, REDEMPTION_STAFF, ADMIN
IF OBJECT_ID('dbo.Principal_Role', 'U') IS NULL
BEGIN
    CREATE TABLE Principal_Role (
        principal_role_id VARCHAR(36) NOT NULL,
        principal_id VARCHAR(36) NOT NULL,
        role_name VARCHAR(30) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_PrincipalRole_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Principal_Role PRIMARY KEY (principal_role_id),
        CONSTRAINT CK_Principal_Role_role_name CHECK (role_name IN ('USER', 'CASHIER_STAFF', 'REDEMPTION_STAFF', 'ADMIN')),
        CONSTRAINT FK_PrincipalRole_Principal FOREIGN KEY (principal_id) REFERENCES Principal(principal_id) ON DELETE CASCADE,
        CONSTRAINT UQ_PrincipalRole_principal_role UNIQUE (principal_id, role_name)
    );
END;

-- 6. Table: Password_Reset_Token (Secure password reset token store)
IF OBJECT_ID('dbo.Password_Reset_Token', 'U') IS NULL
BEGIN
    CREATE TABLE Password_Reset_Token (
        token_id VARCHAR(36) NOT NULL,
        token_hash VARCHAR(255) NOT NULL,
        principal_id VARCHAR(36) NOT NULL,
        expires_at DATETIME2(3) NOT NULL,
        used BIT NOT NULL CONSTRAINT DF_PasswordResetToken_used DEFAULT 0,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_PasswordResetToken_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Password_Reset_Token PRIMARY KEY (token_id),
        CONSTRAINT UQ_PasswordResetToken_token_hash UNIQUE (token_hash),
        CONSTRAINT FK_PasswordResetToken_Principal FOREIGN KEY (principal_id) REFERENCES Principal(principal_id) ON DELETE CASCADE
    );
END;

-- 7. Table: Service_Client (Service-to-service credential store)
IF OBJECT_ID('dbo.Service_Client', 'U') IS NULL
BEGIN
    CREATE TABLE Service_Client (
        client_id VARCHAR(50) NOT NULL,
        client_secret_hash VARCHAR(255) NOT NULL,
        client_name VARCHAR(100) NOT NULL,
        is_active BIT NOT NULL CONSTRAINT DF_ServiceClient_is_active DEFAULT 1,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_ServiceClient_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Service_Client PRIMARY KEY (client_id)
    );
END;

IF OBJECT_ID('dbo.Service_Client_Grant', 'U') IS NULL
BEGIN
    CREATE TABLE Service_Client_Grant (
        grant_id VARCHAR(36) NOT NULL,
        client_id VARCHAR(50) NOT NULL,
        target_service VARCHAR(50) NOT NULL,
        scope VARCHAR(50) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_ServiceClientGrant_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Service_Client_Grant PRIMARY KEY (grant_id),
        CONSTRAINT FK_ServiceClientGrant_Client FOREIGN KEY (client_id) REFERENCES Service_Client(client_id) ON DELETE CASCADE,
        CONSTRAINT UQ_ServiceClientGrant_client_scope UNIQUE (client_id, target_service, scope)
    );
END;

-- Indexes for performance
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Principal_account_type' AND object_id = OBJECT_ID('dbo.Principal'))
    CREATE INDEX IX_Principal_account_type ON Principal(account_type);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Principal_customer_id' AND object_id = OBJECT_ID('dbo.Principal'))
    CREATE INDEX IX_Principal_customer_id ON Principal(customer_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Principal_staff_id' AND object_id = OBJECT_ID('dbo.Principal'))
    CREATE INDEX IX_Principal_staff_id ON Principal(staff_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_PrincipalRole_principal_id' AND object_id = OBJECT_ID('dbo.Principal_Role'))
    CREATE INDEX IX_PrincipalRole_principal_id ON Principal_Role(principal_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ShiftSchedule_staff_id' AND object_id = OBJECT_ID('dbo.Shift_Schedule'))
    CREATE INDEX IX_ShiftSchedule_staff_id ON Shift_Schedule(staff_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ShiftSchedule_shift_date' AND object_id = OBJECT_ID('dbo.Shift_Schedule'))
    CREATE INDEX IX_ShiftSchedule_shift_date ON Shift_Schedule(shift_date);
