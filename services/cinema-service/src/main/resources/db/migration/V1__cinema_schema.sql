-- cinema_db V1: rooms, fixed seat layouts and showtimes (AGENTS 3, 19, 24). Only cinema-local foreign keys:
-- movie_id is a Catalog ID kept by value. Room uses INT IDENTITY; other IDs are VARCHAR(36) so legacy IDs stay unchanged.
-- show_date/start_time/end_time stay the cinema's local display values, as in the source; UTC instants are later work.

CREATE TABLE dbo.Template (
    template_id VARCHAR(36)  NOT NULL CONSTRAINT PK_Template PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(MAX) NULL
);

CREATE TABLE dbo.Template_Seat (
    template_seat_id VARCHAR(36) NOT NULL CONSTRAINT PK_Template_Seat PRIMARY KEY,
    template_id      VARCHAR(36) NOT NULL CONSTRAINT FK_Template_Seat_Template REFERENCES dbo.Template (template_id),
    row_label        VARCHAR(10) NOT NULL,
    seat_number      INT         NOT NULL CONSTRAINT CK_Template_Seat_number CHECK (seat_number > 0),
    seat_type        VARCHAR(20) NOT NULL CONSTRAINT CK_Template_Seat_type CHECK (seat_type IN ('STANDARD', 'VIP')),
    CONSTRAINT UQ_Template_Seat_position UNIQUE (template_id, row_label, seat_number)
);

-- Many rooms may share one template (AGENTS 10).
CREATE TABLE dbo.Room (
    room_id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Room PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL CONSTRAINT FK_Room_Template REFERENCES dbo.Template (template_id),
    status      VARCHAR(20) NOT NULL CONSTRAINT CK_Room_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE dbo.Showtime (
    showtime_id  VARCHAR(36) NOT NULL CONSTRAINT PK_Showtime PRIMARY KEY,
    movie_id     VARCHAR(36) NOT NULL,
    room_id      INT         NOT NULL CONSTRAINT FK_Showtime_Room REFERENCES dbo.Room (room_id),
    show_date    DATE        NOT NULL,
    start_time   TIME(0)     NOT NULL,
    end_time     TIME(0)     NOT NULL,
    sales_status VARCHAR(20) NOT NULL CONSTRAINT DF_Showtime_sales DEFAULT 'DRAFT'
        CONSTRAINT CK_Showtime_sales CHECK (sales_status IN ('DRAFT', 'OPEN', 'CLOSED'))
);
CREATE INDEX IX_Showtime_room_date ON dbo.Showtime (room_id, show_date);
CREATE INDEX IX_Showtime_movie_date ON dbo.Showtime (movie_id, show_date);

-- Kept only while Open Sales still creates these rows; customer availability comes from booking-service.
CREATE TABLE dbo.Showtime_Seat (
    showtime_seat_id VARCHAR(36) NOT NULL CONSTRAINT PK_Showtime_Seat PRIMARY KEY,
    showtime_id      VARCHAR(36) NOT NULL CONSTRAINT FK_Showtime_Seat_Showtime REFERENCES dbo.Showtime (showtime_id),
    template_seat_id VARCHAR(36) NOT NULL CONSTRAINT FK_Showtime_Seat_Template_Seat REFERENCES dbo.Template_Seat (template_seat_id),
    status           VARCHAR(20) NULL,
    CONSTRAINT UQ_Showtime_Seat_template UNIQUE (showtime_id, template_seat_id)
);
