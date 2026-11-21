package vn.edu.fpt.cinema.booking.dto;

import java.time.Instant;
import java.util.List;

/** serverTime lets the client count down from the server clock instead of the device clock. */
public record SeatHoldResponse(String holdId, String showtimeId, List<String> seatIds, Instant expiresAt, Instant serverTime) {}
