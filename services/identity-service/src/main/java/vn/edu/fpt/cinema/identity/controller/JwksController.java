package vn.edu.fpt.cinema.identity.controller;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinema.identity.security.JwtSigningKeyManager;

@RestController
public class JwksController {

    private final JwtSigningKeyManager keyManager;

    public JwksController(JwtSigningKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * Standard OpenID Connect / OAuth2 JWKS endpoint for Gateway and Resource Servers.
     * Complies with AGENTS 1.3 Section 7 & Section 25.
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(keyManager.getJwksPayload());
    }

    @GetMapping(value = "/api/v1/auth/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwksApiAlias() {
        return ResponseEntity.ok(keyManager.getJwksPayload());
    }
}
