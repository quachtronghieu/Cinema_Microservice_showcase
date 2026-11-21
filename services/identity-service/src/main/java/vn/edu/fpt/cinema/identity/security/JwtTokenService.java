package vn.edu.fpt.cinema.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;

@Service
public class JwtTokenService {

    private final JwtSigningKeyManager keyManager;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenService(
            JwtSigningKeyManager keyManager,
            JwtProperties jwtProperties,
            ObjectMapper objectMapper,
            TokenBlacklistService tokenBlacklistService) {
        this.keyManager = keyManager;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Issues an asymmetric RS256 User JWT Token with 15-minute (900s) TTL.
     * Complies strictly with AGENTS 1.3 Section 7 & Section 25.
     */
    public String generateUserToken(Principal principal) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + jwtProperties.getUserTokenTtlSeconds();

        List<String> roleNames = principal.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", principal.getPrincipalId());
        claims.put("iss", jwtProperties.getIssuer());
        claims.put("aud", jwtProperties.getAudience());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("username", principal.getUsername());
        claims.put("accountType", principal.getAccountType().name());
        claims.put("roles", roleNames);
        claims.put("iat", nowSeconds);
        claims.put("nbf", nowSeconds);
        claims.put("exp", expSeconds);

        if (principal.getCustomerId() != null) {
            claims.put("customerId", principal.getCustomerId());
        }
        if (principal.getStaffId() != null) {
            claims.put("staffId", principal.getStaffId());
        }

        return createSignedJwt(claims);
    }

    /**
     * Issues a short-lived (300s / 5-minute) Service-to-Service Token for internal API calls.
     */
    public String generateServiceToken(String clientId, String targetService, String scope) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + jwtProperties.getServiceTokenTtlSeconds();

        Map<String, Object> claims = new HashMap<>();
        // Client ids are stored as "svc:<name>" already; do not prefix twice.
        claims.put("sub", clientId.startsWith("svc:") ? clientId : "svc:" + clientId);
        claims.put("iss", jwtProperties.getIssuer());
        claims.put("aud", targetService);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("token_kind", "service");
        claims.put("scope", scope);
        claims.put("iat", nowSeconds);
        claims.put("nbf", nowSeconds);
        claims.put("exp", expSeconds);

        return createSignedJwt(claims);
    }

    /**
     * Revokes a token upon logout. Adds both token and JTI to blacklist.
     */
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Map<String, Object> claims = parseClaims(token);
        long exp = Instant.now().getEpochSecond() + 900;
        if (claims != null) {
            if (claims.get("exp") instanceof Number) {
                exp = ((Number) claims.get("exp")).longValue();
            }
            String jti = (String) claims.get("jti");
            if (jti != null) {
                tokenBlacklistService.revoke(jti, exp);
            }
        }
        tokenBlacklistService.revoke(token, exp);
    }

    public boolean isTokenRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (tokenBlacklistService.isRevoked(token)) {
            return true;
        }
        Map<String, Object> claims = parseClaims(token);
        if (claims != null && claims.containsKey("jti")) {
            return tokenBlacklistService.isRevoked((String) claims.get("jti"));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean verifyToken(String token) {
        try {
            if (isTokenRevoked(token)) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String signingInput = parts[0] + "." + parts[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyManager.getPublicKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

            if (!signature.verify(signatureBytes)) {
                return false;
            }

            // Check expiration
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<?, ?> claims = objectMapper.readValue(payloadJson, Map.class);
            long exp = ((Number) claims.get("exp")).longValue();
            long now = Instant.now().getEpochSecond();

            // Max clock skew 30 seconds per AGENTS 1.3 Section 25
            return (now - 30) < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private String createSignedJwt(Map<String, Object> claims) {
        try {
            // 1. Build Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", keyManager.getKeyId());

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(claims);

            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;

            // 2. Sign with RS256 (SHA256withRSA)
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyManager.getPrivateKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();

            String encodedSignature = base64UrlEncode(signatureBytes);

            return signingInput + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed JWT token: " + e.getMessage(), e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
