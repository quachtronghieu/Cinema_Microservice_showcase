package vn.edu.fpt.cinema.identity.security;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtSigningKeyManager {

    private static final Logger log = LoggerFactory.getLogger(JwtSigningKeyManager.class);

    private final JwtProperties jwtProperties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public JwtSigningKeyManager(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        try {
            if (jwtProperties.getPrivateKeyPem() != null && !jwtProperties.getPrivateKeyPem().isBlank()) {
                log.info("Loading RSA key pair from configured environment properties...");
                this.privateKey = parsePrivateKey(jwtProperties.getPrivateKeyPem());
                this.publicKey = parsePublicKey(jwtProperties.getPublicKeyPem());
            } else {
                log.info("No external RSA key provided. Generating new RSA 2048-bit KeyPair for identity-service...");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
                this.publicKey = (RSAPublicKey) keyPair.getPublic();
                log.info("RSA 2048-bit KeyPair successfully initialized with kid: {}", jwtProperties.getKeyId());
            }
        } catch (Exception e) {
            log.error("Failed to initialize RSA KeyPair for JWT signing: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot start identity-service without valid RSA signing keys", e);
        }
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return jwtProperties.getKeyId();
    }

    /**
     * Produces standard RFC 7517 JWKS JSON structure for public distribution.
     * Accessible by Gateway and Resource Servers at /.well-known/jwks.json
     */
    public Map<String, Object> getJwksPayload() {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", jwtProperties.getKeyId());

        // Base64URL encode unsigned big-endian modulus n and public exponent e
        jwk.put("n", base64UrlEncodeUnsigned(publicKey.getModulus()));
        jwk.put("e", base64UrlEncodeUnsigned(publicKey.getPublicExponent()));

        return Map.of("keys", List.of(jwk));
    }

    private String base64UrlEncodeUnsigned(BigInteger bigInt) {
        byte[] bytes = bigInt.toByteArray();
        // BigInteger.toByteArray may contain a leading 0x00 sign byte if positive
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
