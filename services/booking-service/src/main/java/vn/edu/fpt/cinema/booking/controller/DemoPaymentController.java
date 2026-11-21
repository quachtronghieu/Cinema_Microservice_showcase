package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.DemoPaymentRequest;
import vn.edu.fpt.cinema.booking.dto.DemoPaymentResponse;
import vn.edu.fpt.cinema.booking.security.CurrentCustomer;
import vn.edu.fpt.cinema.booking.service.PaymentService;

/** Demo-only payment simulator; the endpoint does not exist unless booking.demo-payment.enabled=true (AGENTS 5.3). */
@RestController
@RequestMapping("/api/v1/customer/bookings")
@ConditionalOnProperty(name = "booking.demo-payment.enabled", havingValue = "true")
public class DemoPaymentController {
    private final PaymentService payments;

    public DemoPaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/{bookingId}/demo-payment")
    public DemoPaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable String bookingId,
                                   @Valid @RequestBody DemoPaymentRequest request) {
        return payments.pay(CurrentCustomer.id(jwt), bookingId, request.outcome());
    }
}
