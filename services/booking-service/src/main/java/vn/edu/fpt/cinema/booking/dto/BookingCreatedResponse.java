package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** bookingId/status/totalAmount match the legacy response; expiresAt tells the client when payment closes. */
public record BookingCreatedResponse(String bookingId, String status, BigDecimal totalAmount, Instant expiresAt) {}
