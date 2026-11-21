package vn.edu.fpt.cinema.booking.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.cinema.booking.dto.TicketResponse;
import vn.edu.fpt.cinema.booking.security.CurrentCustomer;
import vn.edu.fpt.cinema.booking.service.BookingQueryService;

@RestController
@RequestMapping("/api/v1/customer/tickets")
public class CustomerTicketController {
    private final BookingQueryService queries;

    public CustomerTicketController(BookingQueryService queries) { this.queries = queries; }

    @GetMapping
    public List<TicketResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return queries.tickets(CurrentCustomer.id(jwt));
    }

    @GetMapping("/{ticketId}")
    public TicketResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String ticketId) {
        return queries.ticket(CurrentCustomer.id(jwt), ticketId);
    }
}
