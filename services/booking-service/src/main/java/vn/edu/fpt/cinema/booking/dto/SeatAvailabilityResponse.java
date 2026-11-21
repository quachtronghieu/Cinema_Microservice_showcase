package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Public view of a showtime's inventory: it must never expose the hold, booking or customer that owns a seat. */
public record SeatAvailabilityResponse(String showtimeId, String salesStatus, Instant serverTime, List<Seat> seats) {
    public record Seat(String showtimeSeatId, String templateSeatId, String rowLabel, int seatNumber,
                       String seatType, String status, BigDecimal unitPrice) {}
}
