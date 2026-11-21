package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.BookingQuoteResponse;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.service.PricingService;
import vn.edu.fpt.cinema.booking.security.CurrentCustomer;

@RestController
@RequestMapping("/api/v1/customer/bookings")
public class QuoteController {
    private final PricingService pricing;

    public QuoteController(PricingService pricing) { this.pricing = pricing; }

    @PostMapping("/preview")
    public BookingQuoteResponse preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CustomerBookingRequest request) {
        return pricing.quote(CurrentCustomer.id(jwt), request).toResponse();
    }
}
