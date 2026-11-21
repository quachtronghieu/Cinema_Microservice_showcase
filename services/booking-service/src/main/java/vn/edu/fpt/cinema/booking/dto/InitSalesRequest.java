package vn.edu.fpt.cinema.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Snapshot cinema-service sends when it opens sales; booking stores it once and sells from it. */
public record InitSalesRequest(
    @NotBlank String showtimeId,
    @NotBlank String movieId,
    @NotBlank String movieTitle,
    @NotNull Integer roomId,
    @NotBlank String roomName,
    @NotNull LocalDate showDate,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotEmpty List<@Valid @NotNull SeatItem> seats
) {
    public record SeatItem(
        @NotBlank String templateSeatId,
        @NotBlank String rowLabel,
        @Positive int seatNumber,
        @NotBlank String seatType,
        // Never a default price: cinema must send the real one (AGENTS 8).
        @NotNull @Positive BigDecimal unitPrice
    ) {}
}
