package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** At most one payment per booking (unique booking_id): the durable guard against paying twice. */
@Entity
@Table(name = "Payment")
public class Payment {
    @Id @Column(name = "payment_id") private String paymentId;
    @Column(name = "booking_id", nullable = false) private String bookingId;
    @Column(name = "method", nullable = false) private String method;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)") private String currency;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected Payment() {}

    /** A finished demo payment: SUCCEEDED or FAILED. */
    public static Payment demo(String bookingId, String status, BigDecimal amount, Instant now) {
        var payment = new Payment();
        payment.paymentId = UUID.randomUUID().toString();
        payment.bookingId = bookingId;
        payment.method = "DEMO";
        payment.status = status;
        payment.amount = amount;
        payment.currency = "VND";
        payment.createdAt = now;
        payment.completedAt = now;
        return payment;
    }

    public String getPaymentId() { return paymentId; }
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCompletedAt() { return completedAt; }
}
