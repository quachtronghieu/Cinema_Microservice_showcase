package vn.edu.fpt.cinemamanagement.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinemamanagement.dto.response.MovieResponseDTO;
import vn.edu.fpt.cinemamanagement.repositories.CustomerRepository;
import vn.edu.fpt.cinemamanagement.services.CustomerWatchlistService;

import java.security.Principal;
import java.util.Map;

/**
 * Watchlist của khách đang đăng nhập.
 * GET /api/customer/watchlist → 200 + danh sách phim; PUT/DELETE /{movieId} → 204 (idempotent),
 * PUT phim không tồn tại → 404 {code, message}.
 */
@RestController
@RequestMapping("/api/customer/watchlist")
public class CustomerWatchlistRestController {
    private final CustomerWatchlistService watchlistService;
    private final CustomerRepository customerRepository;

    public CustomerWatchlistRestController(CustomerWatchlistService watchlistService, CustomerRepository customerRepository) {
        this.watchlistService = watchlistService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(Principal principal) {
        String customerId = customerId(principal);
        if (customerId == null) return forbidden();
        return ResponseEntity.ok(watchlistService.list(customerId).stream().map(MovieResponseDTO::fromEntity).toList());
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<?> add(Principal principal, @PathVariable String movieId) {
        String customerId = customerId(principal);
        if (customerId == null) return forbidden();
        if (!watchlistService.add(customerId, movieId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", "NOT_FOUND", "message", "Không tìm thấy phim."));
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> remove(Principal principal, @PathVariable String movieId) {
        String customerId = customerId(principal);
        if (customerId == null) return forbidden();
        watchlistService.remove(customerId, movieId);
        return ResponseEntity.noContent().build();
    }

    private String customerId(Principal principal) {
        if (principal == null) return null;
        return customerRepository.findByUsername(principal.getName()).map(c -> c.getUser_id()).orElse(null);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "FORBIDDEN", "message", "Tài khoản khách hàng không hợp lệ."));
    }
}
