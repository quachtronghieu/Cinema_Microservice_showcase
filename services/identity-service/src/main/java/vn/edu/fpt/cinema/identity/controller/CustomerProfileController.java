package vn.edu.fpt.cinema.identity.controller;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.identity.application.CustomerProfileService;
import vn.edu.fpt.cinema.identity.dto.CustomerProfileDTO;
import vn.edu.fpt.cinema.identity.dto.CustomerProfileUpdateRequest;
import vn.edu.fpt.cinema.identity.security.JwtTokenService;

@RestController
@RequestMapping("/api/v1/customers/profile")
public class CustomerProfileController {

    private final CustomerProfileService profileService;
    private final JwtTokenService jwtTokenService;

    public CustomerProfileController(CustomerProfileService profileService, JwtTokenService jwtTokenService) {
        this.profileService = profileService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String customerId = customerId(authHeader);
        if (customerId == null) return unauthorized();

        CustomerProfileDTO profile = profileService.getProfileByUserId(customerId);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Customer profile not found."));
        }
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody CustomerProfileUpdateRequest request
    ) {
        String customerId = customerId(authHeader);
        if (customerId == null) return unauthorized();

        Map<String, String> errors = profileService.updateProfile(customerId, request);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }

    // Ownership comes only from the verified token, never from a request header (AGENTS section 7).
    private String customerId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        if (!jwtTokenService.verifyToken(token)) return null;
        Map<String, Object> claims = jwtTokenService.parseClaims(token);
        Object customerId = claims == null ? null : claims.get("customerId");
        return customerId == null || customerId.toString().isBlank() ? null : customerId.toString();
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User identification missing."));
    }
}
