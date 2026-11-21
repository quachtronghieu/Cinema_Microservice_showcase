-- booking_db V1: sales snapshot, seat inventory, holds, bookings, vouchers, payments, tickets, idempotency.
-- AGENTS.md sections 5, 19.3, 24. Only booking-local foreign keys; cinema/catalog/identity data is referenced by ID + snapshot.
-- Times: *_at columns are UTC instants. show_date/start_time/end_time are the cinema's local display values (snapshot only).

CREATE TABLE dbo.Sales_Snapshot (
    showtime_id      VARCHAR(36)   NOT NULL CONSTRAINT PK_Sales_Snapshot PRIMARY KEY,
    snapshot_version BIGINT        NOT NULL,
    sales_status     VARCHAR(10)   NOT NULL CONSTRAINT CK_Sales_Snapshot_status CHECK (sales_status IN ('OPEN', 'CLOSED')),
    movie_id         VARCHAR(36)   NOT NULL,
    movie_title      NVARCHAR(200) NOT NULL,
    room_id          INT           NOT NULL,
    room_name        NVARCHAR(100) NOT NULL,
    show_date        DATE          NOT NULL,
    start_time       TIME(0)       NOT NULL,
    end_time         TIME(0)       NOT NULL,
    created_at       DATETIME2(3)  NOT NULL CONSTRAINT DF_Sales_Snapshot_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.Voucher (
    voucher_id     VARCHAR(36)   NOT NULL CONSTRAINT PK_Voucher PRIMARY KEY,
    code           VARCHAR(20)   NOT NULL CONSTRAINT UQ_Voucher_code UNIQUE,
    voucher_name   NVARCHAR(255) NOT NULL,
    discount_type  VARCHAR(20)   NOT NULL CONSTRAINT CK_Voucher_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    discount_value DECIMAL(19,2) NOT NULL CONSTRAINT CK_Voucher_value CHECK (discount_value > 0),
    usage_limit    INT           NOT NULL CONSTRAINT CK_Voucher_limit CHECK (usage_limit >= 0),
    used_count     INT           NOT NULL CONSTRAINT DF_Voucher_used DEFAULT 0,
    reserved_count INT           NOT NULL CONSTRAINT DF_Voucher_reserved DEFAULT 0,
    status         BIT           NOT NULL CONSTRAINT DF_Voucher_status DEFAULT 1,
    version        BIGINT        NOT NULL CONSTRAINT DF_Voucher_version DEFAULT 0,
    CONSTRAINT CK_Voucher_quota CHECK (used_count >= 0 AND reserved_count >= 0 AND used_count + reserved_count <= usage_limit),
    CONSTRAINT CK_Voucher_percentage CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100)
);

CREATE TABLE dbo.Booking (
    booking_id      VARCHAR(36)   NOT NULL CONSTRAINT PK_Booking PRIMARY KEY,
    customer_id     VARCHAR(36)   NOT NULL,
    showtime_id     VARCHAR(36)   NOT NULL CONSTRAINT FK_Booking_Sales_Snapshot REFERENCES dbo.Sales_Snapshot (showtime_id),
    channel         VARCHAR(10)   NOT NULL CONSTRAINT CK_Booking_channel CHECK (channel IN ('CUSTOMER', 'CASHIER')),
    status          VARCHAR(20)   NOT NULL CONSTRAINT CK_Booking_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED')),
    voucher_id      VARCHAR(36)   NULL CONSTRAINT FK_Booking_Voucher REFERENCES dbo.Voucher (voucher_id),
    subtotal        DECIMAL(19,2) NOT NULL CONSTRAINT CK_Booking_subtotal CHECK (subtotal >= 0),
    discount_amount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Booking_discount DEFAULT 0,
    total_amount    DECIMAL(19,2) NOT NULL CONSTRAINT CK_Booking_total CHECK (total_amount >= 0),
    created_at      DATETIME2(3)  NOT NULL,
    expires_at      DATETIME2(3)  NOT NULL,
    updated_at      DATETIME2(3)  NOT NULL,
    version         BIGINT        NOT NULL CONSTRAINT DF_Booking_version DEFAULT 0,
    CONSTRAINT CK_Booking_discount CHECK (discount_amount >= 0 AND discount_amount <= subtotal)
);
CREATE INDEX IX_Booking_customer_created ON dbo.Booking (customer_id, created_at DESC);
CREATE INDEX IX_Booking_status_expires ON dbo.Booking (status, expires_at);

CREATE TABLE dbo.SeatHold (
    hold_id     VARCHAR(36)  NOT NULL CONSTRAINT PK_SeatHold PRIMARY KEY,
    customer_id VARCHAR(36)  NOT NULL,
    showtime_id VARCHAR(36)  NOT NULL CONSTRAINT FK_SeatHold_Sales_Snapshot REFERENCES dbo.Sales_Snapshot (showtime_id),
    status      VARCHAR(10)  NOT NULL CONSTRAINT CK_SeatHold_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    booking_id  VARCHAR(36)  NULL CONSTRAINT FK_SeatHold_Booking REFERENCES dbo.Booking (booking_id),
    created_at  DATETIME2(3) NOT NULL,
    expires_at  DATETIME2(3) NOT NULL,
    version     BIGINT       NOT NULL CONSTRAINT DF_SeatHold_version DEFAULT 0,
    CONSTRAINT UQ_SeatHold_owner UNIQUE (hold_id, customer_id, showtime_id),
    CONSTRAINT CK_SeatHold_consumed CHECK ((status = 'CONSUMED' AND booking_id IS NOT NULL) OR (status <> 'CONSUMED' AND booking_id IS NULL))
);
CREATE INDEX IX_SeatHold_status_expires ON dbo.SeatHold (status, expires_at);

-- One current hold per customer and showtime; replacing a hold must update this row (serialises concurrent holds).
CREATE TABLE dbo.ActiveHold (
    customer_id VARCHAR(36) NOT NULL,
    showtime_id VARCHAR(36) NOT NULL,
    hold_id     VARCHAR(36) NOT NULL CONSTRAINT UQ_ActiveHold_hold UNIQUE,
    CONSTRAINT PK_ActiveHold PRIMARY KEY (customer_id, showtime_id),
    CONSTRAINT FK_ActiveHold_SeatHold FOREIGN KEY (hold_id, customer_id, showtime_id)
        REFERENCES dbo.SeatHold (hold_id, customer_id, showtime_id)
);

CREATE TABLE dbo.Showtime_Seat (
    showtime_seat_id VARCHAR(36)   NOT NULL CONSTRAINT PK_Showtime_Seat PRIMARY KEY,
    showtime_id      VARCHAR(36)   NOT NULL CONSTRAINT FK_Showtime_Seat_Sales_Snapshot REFERENCES dbo.Sales_Snapshot (showtime_id),
    template_seat_id VARCHAR(36)   NOT NULL,
    row_label        VARCHAR(10)   NOT NULL,
    seat_number      INT           NOT NULL,
    seat_type        VARCHAR(20)   NOT NULL CONSTRAINT CK_Showtime_Seat_type CHECK (seat_type IN ('STANDARD', 'VIP')),
    unit_price       DECIMAL(19,2) NOT NULL CONSTRAINT CK_Showtime_Seat_price CHECK (unit_price > 0),
    status           VARCHAR(10)   NOT NULL CONSTRAINT CK_Showtime_Seat_status CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),
    hold_id          VARCHAR(36)   NULL CONSTRAINT FK_Showtime_Seat_SeatHold REFERENCES dbo.SeatHold (hold_id),
    booking_id       VARCHAR(36)   NULL CONSTRAINT FK_Showtime_Seat_Booking REFERENCES dbo.Booking (booking_id),
    version          BIGINT        NOT NULL CONSTRAINT DF_Showtime_Seat_version DEFAULT 0,
    CONSTRAINT UQ_Showtime_Seat_template UNIQUE (showtime_id, template_seat_id),
    CONSTRAINT UQ_Showtime_Seat_label UNIQUE (showtime_id, row_label, seat_number),
    -- AVAILABLE has no owner; HELD has exactly one owner (hold or booking); SOLD is owned by a booking.
    CONSTRAINT CK_Showtime_Seat_owner CHECK (
        (status = 'AVAILABLE' AND hold_id IS NULL AND booking_id IS NULL)
        OR (status = 'HELD' AND ((hold_id IS NOT NULL AND booking_id IS NULL) OR (hold_id IS NULL AND booking_id IS NOT NULL)))
        OR (status = 'SOLD' AND hold_id IS NULL AND booking_id IS NOT NULL))
);
CREATE INDEX IX_Showtime_Seat_booking ON dbo.Showtime_Seat (booking_id) WHERE booking_id IS NOT NULL;
CREATE INDEX IX_Showtime_Seat_hold ON dbo.Showtime_Seat (hold_id) WHERE hold_id IS NOT NULL;

CREATE TABLE dbo.SeatHold_Item (
    hold_id          VARCHAR(36) NOT NULL CONSTRAINT FK_SeatHold_Item_SeatHold REFERENCES dbo.SeatHold (hold_id),
    showtime_seat_id VARCHAR(36) NOT NULL CONSTRAINT FK_SeatHold_Item_Seat REFERENCES dbo.Showtime_Seat (showtime_seat_id),
    CONSTRAINT PK_SeatHold_Item PRIMARY KEY (hold_id, showtime_seat_id)
);

CREATE TABLE dbo.Booking_Detail (
    booking_detail_id VARCHAR(36)   NOT NULL CONSTRAINT PK_Booking_Detail PRIMARY KEY,
    booking_id        VARCHAR(36)   NOT NULL CONSTRAINT FK_Booking_Detail_Booking REFERENCES dbo.Booking (booking_id),
    item_type         VARCHAR(20)   NOT NULL CONSTRAINT CK_Booking_Detail_type CHECK (item_type IN ('SEAT', 'CONCESSION')),
    showtime_seat_id  VARCHAR(36)   NULL CONSTRAINT FK_Booking_Detail_Seat REFERENCES dbo.Showtime_Seat (showtime_seat_id),
    concession_id     VARCHAR(36)   NULL,
    item_name         NVARCHAR(200) NOT NULL,
    seat_type         VARCHAR(20)   NULL,
    quantity          INT           NOT NULL CONSTRAINT CK_Booking_Detail_quantity CHECK (quantity > 0),
    unit_price        DECIMAL(19,2) NOT NULL CONSTRAINT CK_Booking_Detail_price CHECK (unit_price >= 0),
    total_price       AS (CAST(quantity * unit_price AS DECIMAL(19,2))) PERSISTED,
    CONSTRAINT CK_Booking_Detail_item CHECK (
        (item_type = 'SEAT' AND showtime_seat_id IS NOT NULL AND concession_id IS NULL AND quantity = 1)
        OR (item_type = 'CONCESSION' AND concession_id IS NOT NULL AND showtime_seat_id IS NULL))
);
CREATE INDEX IX_Booking_Detail_booking ON dbo.Booking_Detail (booking_id);
CREATE UNIQUE INDEX UX_Booking_Detail_seat ON dbo.Booking_Detail (booking_id, showtime_seat_id) WHERE showtime_seat_id IS NOT NULL;

-- One reservation per booking, so release/consume can happen exactly once.
CREATE TABLE dbo.Voucher_Reservation (
    booking_id VARCHAR(36)  NOT NULL CONSTRAINT PK_Voucher_Reservation PRIMARY KEY
        CONSTRAINT FK_Voucher_Reservation_Booking REFERENCES dbo.Booking (booking_id),
    voucher_id VARCHAR(36)  NOT NULL CONSTRAINT FK_Voucher_Reservation_Voucher REFERENCES dbo.Voucher (voucher_id),
    status     VARCHAR(10)  NOT NULL CONSTRAINT CK_Voucher_Reservation_status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED')),
    created_at DATETIME2(3) NOT NULL,
    updated_at DATETIME2(3) NOT NULL
);

CREATE TABLE dbo.Payment (
    payment_id   VARCHAR(36)   NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
    booking_id   VARCHAR(36)   NOT NULL CONSTRAINT UQ_Payment_booking UNIQUE
        CONSTRAINT FK_Payment_Booking REFERENCES dbo.Booking (booking_id),
    method       VARCHAR(20)   NOT NULL CONSTRAINT CK_Payment_method CHECK (method IN ('DEMO')),
    status       VARCHAR(10)   NOT NULL CONSTRAINT CK_Payment_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    amount       DECIMAL(19,2) NOT NULL CONSTRAINT CK_Payment_amount CHECK (amount >= 0),
    currency     CHAR(3)       NOT NULL CONSTRAINT DF_Payment_currency DEFAULT 'VND',
    created_at   DATETIME2(3)  NOT NULL,
    completed_at DATETIME2(3)  NULL
);

CREATE TABLE dbo.Ticket (
    ticket_id     VARCHAR(36)  NOT NULL CONSTRAINT PK_Ticket PRIMARY KEY,
    booking_id    VARCHAR(36)  NOT NULL CONSTRAINT UQ_Ticket_booking UNIQUE
        CONSTRAINT FK_Ticket_Booking REFERENCES dbo.Booking (booking_id),
    issued_at     DATETIME2(3) NOT NULL,
    checked_in    BIT          NOT NULL CONSTRAINT DF_Ticket_checked_in DEFAULT 0,
    checked_in_at DATETIME2(3) NULL,
    checked_in_by VARCHAR(36)  NULL,
    CONSTRAINT CK_Ticket_check_in CHECK (
        (checked_in = 0 AND checked_in_at IS NULL AND checked_in_by IS NULL)
        OR (checked_in = 1 AND checked_in_at IS NOT NULL))
);

-- Durable replay store: same actor + operation + key returns the stored result for at least 24 hours.
CREATE TABLE dbo.IdempotencyRecord (
    idempotency_id  BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_IdempotencyRecord PRIMARY KEY,
    actor_id        VARCHAR(36)   NOT NULL,
    operation       VARCHAR(40)   NOT NULL,
    idem_key        VARCHAR(128)  COLLATE Latin1_General_100_BIN2 NOT NULL,
    request_hash    CHAR(64)      NOT NULL,
    resource_id     VARCHAR(36)   NULL,
    response_status INT           NOT NULL,
    response_body   NVARCHAR(MAX) NULL,
    created_at      DATETIME2(3)  NOT NULL,
    expires_at      DATETIME2(3)  NOT NULL,
    CONSTRAINT UQ_IdempotencyRecord_key UNIQUE (actor_id, operation, idem_key)
);
CREATE INDEX IX_IdempotencyRecord_expires ON dbo.IdempotencyRecord (expires_at);
