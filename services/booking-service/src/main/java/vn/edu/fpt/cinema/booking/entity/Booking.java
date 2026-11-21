package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "Booking")
public class Booking {
    @Id @Column(name = "booking_id") private String bookingId;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(name = "showtime_id", nullable = false) private String showtimeId;
    @Column(name = "channel", nullable = false) private String channel;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "voucher_id") private String voucherId;
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2) private BigDecimal subtotal;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2) private BigDecimal discountAmount;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "version", nullable = false) private long version;

    protected Booking() {}

    /** A customer booking waiting for payment until expiresAt; amounts are the server quote at creation time. */
    public static Booking pendingPayment(String bookingId, String customerId, String showtimeId, String voucherId,
                                         BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount,
                                         Instant createdAt, Instant expiresAt) {
        var booking = new Booking();
        booking.bookingId = bookingId;
        booking.customerId = customerId;
        booking.showtimeId = showtimeId;
        booking.channel = "CUSTOMER";
        booking.status = "PENDING_PAYMENT";
        booking.voucherId = voucherId;
        booking.subtotal = subtotal;
        booking.discountAmount = discountAmount;
        booking.totalAmount = totalAmount;
        booking.createdAt = createdAt;
        booking.expiresAt = expiresAt;
        booking.updatedAt = createdAt;
        return booking;
    }

    public String getBookingId() { return bookingId; }
    public String getCustomerId() { return customerId; }
    public String getShowtimeId() { return showtimeId; }
    public String getVoucherId() { return voucherId; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getExpiresAt() { return expiresAt; }
}
