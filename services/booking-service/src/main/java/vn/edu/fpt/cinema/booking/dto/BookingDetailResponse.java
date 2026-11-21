package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Built only from booking_db snapshots: prices and names are those at purchase time. */
public record BookingDetailResponse(
    String bookingId,
    String status,
    Instant createdAt,
    Instant expiresAt,
    String movieTitle,
    String roomName,
    LocalDate showDate,
    LocalTime startTime,
    List<Seat> seats,
    List<Concession> concessions,
    String voucherCode,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    Payment payment,
    String ticketId
) {
    public record Seat(String label, String seatType, BigDecimal unitPrice) {}

    public record Concession(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record Payment(String paymentId, String status, Instant paidAt) {}
}
