package vn.edu.fpt.cinema.identity.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinema.identity.application.PasswordResetService;
import vn.edu.fpt.cinema.identity.application.ServiceClientService;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.dto.ErrorResponse;
import vn.edu.fpt.cinema.identity.dto.ForgotPasswordRequest;
import vn.edu.fpt.cinema.identity.dto.LoginRequest;
import vn.edu.fpt.cinema.identity.dto.LoginResponse;
import vn.edu.fpt.cinema.identity.dto.ResetPasswordRequest;
import vn.edu.fpt.cinema.identity.dto.ServiceTokenRequest;
import vn.edu.fpt.cinema.identity.dto.ServiceTokenResponse;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.security.JwtTokenService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final PrincipalRepository principalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PasswordResetService passwordResetService;
    private final ServiceClientService serviceClients;

    public AuthController(
            PrincipalRepository principalRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            PasswordResetService passwordResetService,
            ServiceClientService serviceClients) {
        this.principalRepository = principalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.passwordResetService = passwordResetService;
        this.serviceClients = serviceClients;
    }

    /**
     * User authentication endpoint (Customers & Staff).
     * Accessible via both /api/v1/auth/login (Microservice standard) and /api/auth/login (Flutter app).
     * Issues an asymmetric RS256 JWT Token with 15-minute TTL.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        Principal principal = principalRepository.findByNormalizedUsername(normalizedUsername).orElse(null);
        if (principal == null || !passwordEncoder.matches(request.getPassword(), principal.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                    "AUTH_INVALID_CREDENTIALS",
                    "Invalid username or password"
            ));
        }

        if (!Boolean.TRUE.equals(principal.getIsActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                    "AUTH_ACCOUNT_DEACTIVATED",
                    "Account has been deactivated. Please contact support."
            ));
        }

        String token = jwtTokenService.generateUserToken(principal);

        List<String> roleNames = principal.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .toList();

        String domainUserId = principal.getCustomerId() != null
                ? principal.getCustomerId()
                : (principal.getStaffId() != null ? String.valueOf(principal.getStaffId()) : principal.getPrincipalId());

        LoginResponse response = new LoginResponse(
                token,
                900,
                principal.getPrincipalId(),
                principal.getUsername(),
                principal.getAccountType().name(),
                roleNames,
                domainUserId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * User logout endpoint (UC5).
     * Revokes JWT token via token blacklist, invalidates session, clears cookies.
     * Dual-routed to /api/v1/auth/logout and /api/auth/logout. Idempotent.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            jwtTokenService.revokeToken(token);
        }

        // Invalidate HttpSession if present
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear SecurityContext
        SecurityContextHolder.clearContext();

        // Expire session cookie
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    /**
     * Current authenticated user profile endpoint.
     * Resolves principal from valid non-revoked Bearer token.
     * Dual-routed to /api/v1/auth/me and /api/auth/me.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTH_UNAUTHORIZED", "Not authenticated."));
        }

        String token = authHeader.substring(7).trim();
        if (!jwtTokenService.verifyToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTH_INVALID_TOKEN", "Invalid or expired token."));
        }

        Map<String, Object> claims = jwtTokenService.parseClaims(token);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTH_INVALID_TOKEN", "Unable to parse token claims."));
        }

        String principalId = (String) claims.get("sub");
        String username = (String) claims.get("username");
        String accountType = (String) claims.get("accountType");
        String customerId = (String) claims.get("customerId");
        String staffId = (String) claims.get("staffId");

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getOrDefault("roles", List.of("USER"));

        String domainUserId = customerId != null ? customerId : (staffId != null ? staffId : principalId);

        long exp = claims.get("exp") instanceof Number ? ((Number) claims.get("exp")).longValue() : (Instant.now().getEpochSecond() + 900);
        long remainingTtl = Math.max(0, exp - Instant.now().getEpochSecond());

        LoginResponse userInfo = new LoginResponse(
                token,
                remainingTtl,
                principalId,
                username,
                accountType,
                roles,
                domainUserId
        );

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Internal service authentication endpoint (Client Credentials).
     * Issues a short-lived RS256 Service Token (5-minute TTL) for service-to-service communication.
     */
    @PostMapping("/token/service")
    public ResponseEntity<?> issueServiceToken(@Valid @RequestBody ServiceTokenRequest request) {
        // This route is public (it is how services log in), so the secret and the grant are checked here.
        if (!serviceClients.authenticate(request.getClientId(), request.getClientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                    "SERVICE_CLIENT_INVALID", "Invalid service client credentials"));
        }
        if (!serviceClients.hasGrant(request.getClientId(), request.getTargetService(), request.getScope())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                    "SERVICE_SCOPE_NOT_GRANTED", "This client is not granted that scope on that service"));
        }
        String token = jwtTokenService.generateServiceToken(
                request.getClientId(),
                request.getTargetService(),
                request.getScope()
        );

        return ResponseEntity.ok(new ServiceTokenResponse(token, 300, request.getScope()));
    }

    /**
     * Password reset request endpoint (Row 12 Backlog / UC6).
     * Generates a secure reset token, creates Password_Reset_Token record, builds reset link.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request));
    }

    /**
     * Password reset execution endpoint (Row 12 Backlog / UC6).
     * Validates reset token and expiration, updates BCrypt password hash for Customer and Principal.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}
