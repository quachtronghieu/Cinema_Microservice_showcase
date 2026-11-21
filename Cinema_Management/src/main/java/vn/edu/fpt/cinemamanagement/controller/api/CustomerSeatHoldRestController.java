package vn.edu.fpt.cinemamanagement.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinemamanagement.dto.request.SeatHoldRequestDTO;
import vn.edu.fpt.cinemamanagement.exception.SeatHoldException;
import vn.edu.fpt.cinemamanagement.repositories.CustomerRepository;
import vn.edu.fpt.cinemamanagement.services.CustomerSeatHoldService;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Giữ/nhả ghế của khách hàng (UC-16).
 *
 * <ul>
 *   <li>{@code POST /api/customer/seat-holds} → 201 + hold</li>
 *   <li>{@code DELETE /api/customer/seat-holds/{holdId}} → 204</li>
 * </ul>
 * Lỗi trả {@code {"code", "message", "seatIds"?}}; 409 {@code SEAT_UNAVAILABLE}
 * kèm {@code seatIds} của ghế vừa bị người khác giữ/đặt.
 */
@RestController
@RequestMapping("/api/customer/seat-holds")
public class CustomerSeatHoldRestController {
    private final CustomerSeatHoldService seatHoldService;
    private final CustomerRepository customerRepository;

    public CustomerSeatHoldRestController(
            CustomerSeatHoldService seatHoldService,
            CustomerRepository customerRepository
    ) {
        this.seatHoldService = seatHoldService;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<?> hold(Principal principal, @RequestBody SeatHoldRequestDTO request) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(seatHoldService.hold(customerId, request));
        } catch (SeatHoldException exception) {
            return error(exception);
        }
    }

    @DeleteMapping("/{holdId}")
    public ResponseEntity<?> release(Principal principal, @PathVariable String holdId) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        try {
            seatHoldService.release(customerId, holdId);
            return ResponseEntity.noContent().build();
        } catch (SeatHoldException exception) {
            return error(exception);
        }
    }

    private String customerId(Principal principal) {
        if (principal == null) return null;
        return customerRepository.findByUsername(principal.getName()).map(customer -> customer.getUser_id()).orElse(null);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "FORBIDDEN",
                "message", "Tài khoản khách hàng không hợp lệ."
        ));
    }

    private ResponseEntity<Map<String, Object>> error(SeatHoldException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.getCode());
        body.put("message", exception.getMessage());
        if (!exception.getSeatIds().isEmpty()) body.put("seatIds", exception.getSeatIds());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
