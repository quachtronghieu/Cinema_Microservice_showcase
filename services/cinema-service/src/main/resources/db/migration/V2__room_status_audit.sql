-- History of room status changes (docs/ngan-task-scope.md, R-06). Rows are only inserted, never updated.
CREATE TABLE dbo.Room_Status_Audit (
    audit_id   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Room_Status_Audit PRIMARY KEY,
    room_id    INT          NOT NULL CONSTRAINT FK_Room_Status_Audit_Room REFERENCES dbo.Room (room_id),
    old_status VARCHAR(20)  NOT NULL,
    new_status VARCHAR(20)  NOT NULL,
    actor_id   VARCHAR(64)  NOT NULL,
    trace_id   VARCHAR(64)  NOT NULL,
    changed_at DATETIME2(3) NOT NULL CONSTRAINT DF_Room_Status_Audit_changed DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_Room_Status_Audit_room ON dbo.Room_Status_Audit (room_id, changed_at);
