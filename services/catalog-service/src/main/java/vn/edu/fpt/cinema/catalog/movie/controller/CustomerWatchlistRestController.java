package vn.edu.fpt.cinema.catalog.movie.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.MovieResponse;
import vn.edu.fpt.cinema.catalog.movie.service.CustomerWatchlistService;

@RestController
@RequestMapping("/api/v1/customer/watchlist")
public class CustomerWatchlistRestController {

    private final CustomerWatchlistService watchlistService;

    public CustomerWatchlistRestController(CustomerWatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<MovieResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return watchlistService.list(customerId(jwt));
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal Jwt jwt, @PathVariable String movieId) {
        if (!watchlistService.add(customerId(jwt), movieId)) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", "Movie not found");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String movieId) {
        watchlistService.remove(customerId(jwt), movieId);
        return ResponseEntity.noContent().build();
    }

    // Ownership comes only from the verified token, never from a request header (AGENTS section 7).
    private static String customerId(Jwt jwt) {
        String customerId = jwt.getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new CatalogException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only customer accounts have a watchlist");
        }
        return customerId;
    }
}
