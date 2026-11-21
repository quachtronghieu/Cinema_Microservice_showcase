package vn.edu.fpt.cinema.cinema.support;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import vn.edu.fpt.cinema.cinema.config.SecurityConfiguration;

/** Signs real RS256 tokens for tests; the matching decoder replaces the JWKS one. */
public final class TestJwt {
    public static final String ISSUER = "https://issuer.test";
    public static final String AUDIENCE = "cinema-api";
    private static final KeyPair PAIR = keyPair();
    private TestJwt() {}

    @TestConfiguration
    public static class Keys {
        @Bean @Primary JwtDecoder testJwtDecoder() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) PAIR.getPublic()).build();
            decoder.setJwtValidator(SecurityConfiguration.validators(ISSUER, AUDIENCE));
            return decoder;
        }
    }

    public static String customer(String customerId) { return bearer("USER", Map.of("customerId", customerId)); }
    public static String admin(String staffId) { return bearer("ADMIN", Map.of("staffId", staffId)); }

    public static String bearer(String role, Map<String, Object> extraClaims) {
        try {
            var builder = new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString()).issuer(ISSUER)
                .audience(AUDIENCE).issueTime(new Date()).notBeforeTime(Date.from(Instant.now().minusSeconds(1)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("roles", List.of(role));
            extraClaims.forEach(builder::claim);
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), builder.build());
            jwt.sign(new RSASSASigner((RSAPrivateKey) PAIR.getPrivate()));
            return "Bearer " + jwt.serialize();
        } catch (JOSEException e) { throw new IllegalStateException(e); }
    }

    private static KeyPair keyPair() {
        try { var g = KeyPairGenerator.getInstance("RSA"); g.initialize(2048); return g.generateKeyPair(); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
