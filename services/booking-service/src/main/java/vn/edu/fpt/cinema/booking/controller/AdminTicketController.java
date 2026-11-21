package vn.edu.fpt.cinema.booking.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.CheckInResponse;
import vn.edu.fpt.cinema.booking.security.CurrentStaff;
import vn.edu.fpt.cinema.booking.service.TicketCheckInService;

@RestController
@RequestMapping("/api/v1/admin/tickets")
public class AdminTicketController {
    private final TicketCheckInService checkIns;

    public AdminTicketController(TicketCheckInService checkIns) { this.checkIns = checkIns; }

    @PostMapping("/{ticketId}/check-in")
    public CheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable String ticketId) {
        return checkIns.checkIn(ticketId, CurrentStaff.id(jwt));
    }
}
