package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** One purchased booking in "My bookings"; open its detail for seats, concessions and payment. */
public record BookingSummaryItem(String bookingId, String status, String movieTitle, LocalDate showDate,
                                 LocalTime startTime, List<String> seatLabels, BigDecimal totalAmount, Instant createdAt) {}
