package vn.edu.fpt.cinema.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "Password_Reset_Token")
public class PasswordResetToken {

    @Id
    @Column(name = "token_id", length = 36)
    private String tokenId;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_id", nullable = false)
    private Principal principal;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken() {}

    public PasswordResetToken(String tokenId, String tokenHash, Principal principal, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.tokenHash = tokenHash;
        this.principal = principal;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Principal getPrincipal() { return principal; }
    public void setPrincipal(Principal principal) { this.principal = principal; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
