package vn.edu.fpt.cinema.booking.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** Field names match the legacy customer API (AGENTS 18.1). The customer comes from the token, never from the body. */
public record SeatHoldRequest(
    @NotBlank @Size(max = 36) String showtimeId,
    @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 36) String> showtimeSeatIds
) {}
