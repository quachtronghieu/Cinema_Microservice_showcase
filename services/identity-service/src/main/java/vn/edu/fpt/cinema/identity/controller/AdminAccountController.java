package vn.edu.fpt.cinema.identity.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.fpt.cinema.identity.application.AdminAccountService;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;
import vn.edu.fpt.cinema.identity.dto.AccountResponse;
import vn.edu.fpt.cinema.identity.dto.AdminResetPasswordRequest;
import vn.edu.fpt.cinema.identity.dto.UpdateAccountRolesRequest;
import vn.edu.fpt.cinema.identity.dto.UpdateAccountStatusRequest;
import vn.edu.fpt.cinema.identity.security.JwtTokenService;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;
    private final JwtTokenService jwtTokenService;

    public AdminAccountController(
            AdminAccountService adminAccountService,
            JwtTokenService jwtTokenService) {
        this.adminAccountService = adminAccountService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> listAccounts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validateAdminCaller(authHeader);

        Page<AccountResponse> result = adminAccountService.listAccounts(search, accountType, role, isActive, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{principalId}")
    public ResponseEntity<AccountResponse> getAccountDetail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable String principalId) {

        validateAdminCaller(authHeader);

        AccountResponse result = adminAccountService.getAccountDetail(principalId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{principalId}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable String principalId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        String actorPrincipalId = validateAdminCaller(authHeader);

        AccountResponse result = adminAccountService.updateAccountStatus(actorPrincipalId, principalId, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{principalId}/roles")
    public ResponseEntity<AccountResponse> updateAccountRoles(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable String principalId,
            @Valid @RequestBody UpdateAccountRolesRequest request) {

        String actorPrincipalId = validateAdminCaller(authHeader);

        AccountResponse result = adminAccountService.updateAccountRoles(actorPrincipalId, principalId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{principalId}/reset-password")
    public ResponseEntity<Map<String, Object>> adminResetPassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable String principalId,
            @Valid @RequestBody AdminResetPasswordRequest request) {

        String actorPrincipalId = validateAdminCaller(authHeader);

        Map<String, Object> result = adminAccountService.adminResetPassword(actorPrincipalId, principalId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Enforces strict RBAC and privilege escalation protection:
     * 1. Requires valid Bearer JWT.
     * 2. Rejects revoked / expired tokens.
     * 3. Requires caller to possess the 'ADMIN' role.
     * 4. Returns caller's principal ID (sub).
     */
    private String validateAdminCaller(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required. Bearer token missing.");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtTokenService.verifyToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }

        Map<String, Object> claims = jwtTokenService.parseClaims(token);
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to parse authentication token.");
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        if (roles == null || !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Admin role required.");
        }

        return (String) claims.get("sub");
    }
}
