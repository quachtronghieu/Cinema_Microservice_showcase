package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One voucher use held by a booking; its status lets cancel/payment release or consume it exactly once. */
@Entity
@Table(name = "Voucher_Reservation")
public class VoucherReservation {
    @Id @Column(name = "booking_id") private String bookingId;
    @Column(name = "voucher_id", nullable = false) private String voucherId;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected VoucherReservation() {}

    public VoucherReservation(String bookingId, String voucherId, Instant now) {
        this.bookingId = bookingId;
        this.voucherId = voucherId;
        this.status = "RESERVED";
        this.createdAt = now;
        this.updatedAt = now;
    }
}
