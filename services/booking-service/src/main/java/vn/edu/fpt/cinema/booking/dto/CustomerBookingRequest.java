package vn.edu.fpt.cinema.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** Field names match the Flutter client and the legacy customer API (AGENTS 18.1). Prices never come from the client. */
public record CustomerBookingRequest(
    @NotBlank String holdId,
    @NotBlank String showtimeId,
    @NotNull @Size(min = 1, max = 8) List<@NotBlank String> showtimeSeatIds,
    List<@Valid ConcessionItem> concessions,
    @Size(max = 20) String voucherCode
) {
    public List<ConcessionItem> concessionsOrEmpty() { return concessions == null ? List.of() : concessions; }

    public record ConcessionItem(@NotBlank String concessionId, @NotNull @Min(1) @Max(99) Integer quantity) {}
}
