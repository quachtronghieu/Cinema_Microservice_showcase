package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "SeatHold")
public class SeatHold {
    @Id @Column(name = "hold_id") private String holdId;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(name = "showtime_id", nullable = false) private String showtimeId;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    protected SeatHold() {}

    public SeatHold(String holdId, String customerId, String showtimeId, String status, Instant expiresAt) {
        this.holdId = holdId;
        this.customerId = customerId;
        this.showtimeId = showtimeId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public static SeatHold active(String holdId, String customerId, String showtimeId, Instant createdAt, Instant expiresAt) {
        SeatHold hold = new SeatHold(holdId, customerId, showtimeId, "ACTIVE", expiresAt);
        hold.createdAt = createdAt;
        return hold;
    }

    public String getHoldId() { return holdId; }
    public String getCustomerId() { return customerId; }
    public String getShowtimeId() { return showtimeId; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }

    /** A hold can be priced or consumed only while ACTIVE and strictly before its expiry instant. */
    public boolean isUsableAt(Instant now) { return "ACTIVE".equals(status) && now.isBefore(expiresAt); }
}
