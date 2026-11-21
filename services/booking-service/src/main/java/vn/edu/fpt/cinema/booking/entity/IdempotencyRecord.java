package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Stored result of a retryable command, replayed for the same (actor, operation, key) and request hash. */
@Entity
@Table(name = "IdempotencyRecord")
public class IdempotencyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "idempotency_id") private Long id;
    @Column(name = "actor_id", nullable = false) private String actorId;
    @Column(name = "operation", nullable = false) private String operation;
    @Column(name = "idem_key", nullable = false) private String idemKey;
    @Column(name = "request_hash", nullable = false, columnDefinition = "char(64)") private String requestHash;
    @Column(name = "resource_id") private String resourceId;
    @Column(name = "response_status", nullable = false) private int responseStatus;
    @Column(name = "response_body", columnDefinition = "nvarchar(max)") private String responseBody;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String actorId, String operation, String idemKey, String requestHash, String resourceId,
                             int responseStatus, String responseBody, Instant createdAt, Instant expiresAt) {
        this.actorId = actorId;
        this.operation = operation;
        this.idemKey = idemKey;
        this.requestHash = requestHash;
        this.resourceId = resourceId;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getRequestHash() { return requestHash; }
    public String getResponseBody() { return responseBody; }
    public Instant getExpiresAt() { return expiresAt; }
}
