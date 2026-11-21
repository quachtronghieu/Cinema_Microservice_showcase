package vn.edu.fpt.cinema.booking.support;

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
import vn.edu.fpt.cinema.booking.config.SecurityConfiguration;

/** Signs real RS256 tokens for tests; the matching decoder replaces the JWKS one. */
public final class TestJwt {
    public static final String ISSUER = "https://issuer.test";
    public static final String AUDIENCE = "cinema-api";
    public static final String SERVICE_AUDIENCE = "booking-service";
    private static final KeyPair PAIR = keyPair();
    private TestJwt() {}

    @TestConfiguration
    public static class Keys {
        @Bean @Primary JwtDecoder testJwtDecoder() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) PAIR.getPublic()).build();
            decoder.setJwtValidator(SecurityConfiguration.validators(ISSUER, AUDIENCE, SERVICE_AUDIENCE));
            return decoder;
        }
    }

    public static String customer(String customerId) { return bearer("USER", Map.of("customerId", customerId)); }
    public static String admin(String staffId) { return bearer("ADMIN", Map.of("staffId", staffId)); }

    /** Service token as identity issues it: token_kind=service, audience = the receiving service, no roles. */
    public static String service(String audience, String scope) {
        return sign(new JWTClaimsSet.Builder().subject("svc:cinema-service").audience(audience)
            .claim("token_kind", "service").claim("scope", scope));
    }

    public static String bearer(String role, Map<String, Object> extraClaims) {
        var builder = new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString())
            .audience(AUDIENCE).claim("roles", List.of(role));
        extraClaims.forEach(builder::claim);
        return sign(builder);
    }

    private static String sign(JWTClaimsSet.Builder builder) {
        try {
            builder.issuer(ISSUER).issueTime(new Date()).notBeforeTime(Date.from(Instant.now().minusSeconds(1)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
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
