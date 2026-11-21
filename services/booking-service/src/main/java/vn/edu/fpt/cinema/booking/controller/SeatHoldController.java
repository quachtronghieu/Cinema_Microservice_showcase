package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.SeatHoldRequest;
import vn.edu.fpt.cinema.booking.dto.SeatHoldResponse;
import vn.edu.fpt.cinema.booking.security.CurrentCustomer;
import vn.edu.fpt.cinema.booking.service.SeatHoldService;

@RestController
@RequestMapping("/api/v1/customer/seat-holds")
public class SeatHoldController {
    private final SeatHoldService holds;

    public SeatHoldController(SeatHoldService holds) { this.holds = holds; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeatHoldResponse hold(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SeatHoldRequest request) {
        return holds.hold(CurrentCustomer.id(jwt), request);
    }

    @DeleteMapping("/{holdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@AuthenticationPrincipal Jwt jwt, @PathVariable String holdId) {
        holds.release(CurrentCustomer.id(jwt), holdId);
    }
}
