package vn.edu.fpt.cinema.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cinema.jwt")
public class JwtProperties {

    private String issuer = "cinema-identity";
    private String audience = "cinema-api";
    private String keyId = "cinema-rsa-key-v1";
    private long userTokenTtlSeconds = 900;       // 15 minutes as per AGENTS 1.3 Section 25
    private long serviceTokenTtlSeconds = 300;    // 5 minutes as per AGENTS 1.3 Section 25
    private String privateKeyPem;
    private String publicKeyPem;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public long getUserTokenTtlSeconds() { return userTokenTtlSeconds; }
    public void setUserTokenTtlSeconds(long userTokenTtlSeconds) { this.userTokenTtlSeconds = userTokenTtlSeconds; }

    public long getServiceTokenTtlSeconds() { return serviceTokenTtlSeconds; }
    public void setServiceTokenTtlSeconds(long serviceTokenTtlSeconds) { this.serviceTokenTtlSeconds = serviceTokenTtlSeconds; }

    public String getPrivateKeyPem() { return privateKeyPem; }
    public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }

    public String getPublicKeyPem() { return publicKeyPem; }
    public void setPublicKeyPem(String publicKeyPem) { this.publicKeyPem = publicKeyPem; }
}
