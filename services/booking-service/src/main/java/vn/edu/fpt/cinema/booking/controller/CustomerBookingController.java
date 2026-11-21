package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.BookingCreatedResponse;
import vn.edu.fpt.cinema.booking.dto.BookingDetailResponse;
import vn.edu.fpt.cinema.booking.dto.BookingStatusResponse;
import vn.edu.fpt.cinema.booking.dto.BookingSummaryItem;
import vn.edu.fpt.cinema.booking.dto.PageResponse;
import vn.edu.fpt.cinema.booking.service.BookingQueryService;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.security.CurrentCustomer;
import vn.edu.fpt.cinema.booking.service.BookingService;

@RestController
@RequestMapping("/api/v1/customer/bookings")
public class CustomerBookingController {
    private final BookingService bookings;
    private final BookingQueryService queries;

    public CustomerBookingController(BookingService bookings, BookingQueryService queries) {
        this.bookings = bookings;
        this.queries = queries;
    }

    @GetMapping
    public PageResponse<BookingSummaryItem> list(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return queries.list(CurrentCustomer.id(jwt), page, size);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingStatusResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String bookingId) {
        return bookings.cancel(CurrentCustomer.id(jwt), bookingId);
    }

    @GetMapping("/{bookingId}")
    public BookingDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String bookingId) {
        return queries.detail(CurrentCustomer.id(jwt), bookingId);
    }

    @PostMapping
    public ResponseEntity<BookingCreatedResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                         @Valid @RequestBody CustomerBookingRequest request) {
        var result = bookings.create(CurrentCustomer.id(jwt), idempotencyKey, request);
        var location = URI.create("/api/v1/customer/bookings/" + result.body().bookingId());
        // 201 for the first submission, 200 when the same key and payload are replayed (AGENTS 23).
        return result.replayed()
            ? ResponseEntity.ok().location(location).body(result.body())
            : ResponseEntity.created(location).body(result.body());
    }
}
