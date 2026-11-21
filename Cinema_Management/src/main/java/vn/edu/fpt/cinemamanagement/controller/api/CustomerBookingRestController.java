package vn.edu.fpt.cinemamanagement.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinemamanagement.dto.request.CustomerBookingRequestDTO;
import vn.edu.fpt.cinemamanagement.dto.request.DemoPaymentRequestDTO;
import vn.edu.fpt.cinemamanagement.dto.response.CustomerBookingCreatedResponseDTO;
import vn.edu.fpt.cinemamanagement.exception.SeatHoldException;
import vn.edu.fpt.cinemamanagement.repositories.CustomerRepository;
import vn.edu.fpt.cinemamanagement.services.CustomerBookingService;
import vn.edu.fpt.cinemamanagement.services.CustomerDemoPaymentService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingRestController {
    private static final int IDEMPOTENCY_CACHE_LIMIT = 500;

    private final CustomerBookingService bookingService;
    private final CustomerDemoPaymentService demoPaymentService;
    private final CustomerRepository customerRepository;
    private final Map<String, IdempotencyEntry> idempotencyResponses = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, IdempotencyEntry> eldest) {
            return size() > IDEMPOTENCY_CACHE_LIMIT;
        }
    };

    public CustomerBookingRestController(
            CustomerBookingService bookingService,
            CustomerDemoPaymentService demoPaymentService,
            CustomerRepository customerRepository
    ) {
        this.bookingService = bookingService;
        this.demoPaymentService = demoPaymentService;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/{bookingId}/demo-payment")
    public ResponseEntity<?> processDemoPayment(
            Principal principal,
            @PathVariable String bookingId,
            @RequestBody DemoPaymentRequestDTO request
    ) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        try {
            return ResponseEntity.ok(
                    demoPaymentService.process(customerId, bookingId, request == null ? null : request.getOutcome())
            );
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (java.util.NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(Principal principal, @RequestBody CustomerBookingRequestDTO request) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        try {
            return ResponseEntity.ok(bookingService.preview(customerId, request));
        } catch (SeatHoldException exception) {
            return holdError(exception);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            Principal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerBookingRequestDTO request
    ) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Idempotency-Key là bắt buộc và không được dài quá 128 ký tự."
            ));
        }
        try {
            String cacheKey = customerId + ':' + idempotencyKey.trim();
            String fingerprint = requestFingerprint(request);
            synchronized (idempotencyResponses) {
                IdempotencyEntry previous = idempotencyResponses.get(cacheKey);
                if (previous != null) {
                    if (!previous.fingerprint().equals(fingerprint)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                                "message", "Idempotency-Key này đã được dùng cho một yêu cầu khác."
                        ));
                    }
                    return ResponseEntity.ok(previous.response());
                }

                CustomerBookingCreatedResponseDTO created = bookingService.createBooking(customerId, request);
                idempotencyResponses.put(cacheKey, new IdempotencyEntry(fingerprint, created));
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
            }
        } catch (SeatHoldException exception) {
            return holdError(exception);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private String customerId(Principal principal) {
        if (principal == null) return null;
        return customerRepository.findByUsername(principal.getName()).map(customer -> customer.getUser_id()).orElse(null);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Tài khoản khách hàng không hợp lệ."));
    }

    /**
     * The existing database has no idempotency table. This bounded, in-memory
     * cache prevents duplicate taps/retries while this server instance is alive.
     */
    private String requestFingerprint(CustomerBookingRequestDTO request) {
        if (request == null) return "null";
        var concessions = new ArrayList<String>();
        if (request.getConcessions() != null) {
            for (CustomerBookingRequestDTO.ConcessionItemDTO item : request.getConcessions()) {
                concessions.add(item == null
                        ? "null"
                        : item.getConcessionId() + ":" + item.getQuantity());
            }
        }
        return String.join("|", String.valueOf(request.getShowtimeId()),
                String.valueOf(request.getHoldId()),
                String.valueOf(request.getShowtimeSeatIds()),
                String.join(",", concessions), String.valueOf(request.getVoucherCode()));
    }

    private ResponseEntity<Map<String, Object>> holdError(SeatHoldException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.getCode());
        body.put("message", exception.getMessage());
        if (!exception.getSeatIds().isEmpty()) body.put("seatIds", exception.getSeatIds());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    private record IdempotencyEntry(String fingerprint, CustomerBookingCreatedResponseDTO response) {}

}
