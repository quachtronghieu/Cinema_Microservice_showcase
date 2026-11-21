package vn.edu.fpt.cinema.booking.controller;

import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.SeatAvailabilityResponse;
import vn.edu.fpt.cinema.booking.service.SeatHoldService;

/** Public read of the real inventory; guests can see it, holding seats needs a login. */
@RestController
public class SeatAvailabilityController {
    private final SeatHoldService holds;

    public SeatAvailabilityController(SeatHoldService holds) { this.holds = holds; }

    @GetMapping("/api/v1/showtimes/{showtimeId}/seat-availability")
    public SeatAvailabilityResponse availability(@PathVariable String showtimeId) {
        return holds.availability(showtimeId);
    }
}
