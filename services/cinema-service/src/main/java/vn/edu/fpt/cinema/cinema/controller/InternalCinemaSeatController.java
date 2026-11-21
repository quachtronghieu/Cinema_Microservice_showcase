package vn.edu.fpt.cinema.cinema.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.cinema.enums.SeatStatus;
import vn.edu.fpt.cinema.cinema.service.ShowtimeSeatService;

@RestController
@RequestMapping("/api/v1/cinema/internal/seats")
public class InternalCinemaSeatController {

    private final ShowtimeSeatService showtimeSeatService;

    public InternalCinemaSeatController(ShowtimeSeatService showtimeSeatService) {
        this.showtimeSeatService = showtimeSeatService;
    }

    @PostMapping("/lock")
    public ResponseEntity<?> lockSeats(
            @RequestParam String showtimeId,
            @RequestBody List<String> seatIds
    ) {
        boolean success = showtimeSeatService.updateSeatStatus(showtimeId, seatIds, SeatStatus.UNAVAILABLE);
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to lock seats"));
        }
        return ResponseEntity.ok(Map.of("message", "Seats locked successfully"));
    }

    @PostMapping("/release")
    public ResponseEntity<?> releaseSeats(
            @RequestParam String showtimeId,
            @RequestBody List<String> seatIds
    ) {
        boolean success = showtimeSeatService.updateSeatStatus(showtimeId, seatIds, SeatStatus.AVAILABLE);
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to release seats"));
        }
        return ResponseEntity.ok(Map.of("message", "Seats released successfully"));
    }
}

