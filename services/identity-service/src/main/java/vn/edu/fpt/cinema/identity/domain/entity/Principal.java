package vn.edu.fpt.cinema.identity.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;

@Entity
@Table(name = "Principal")
public class Principal {

    @Id
    @Column(name = "principal_id", length = 36)
    private String principalId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "normalized_username", nullable = false, length = 100, unique = true)
    private String normalizedUsername;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "staff_id", length = 36)
    private String staffId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "principal", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<PrincipalRole> roles = new HashSet<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Principal() {}

    public Principal(String principalId, String username, String normalizedUsername, String passwordHash, AccountType accountType) {
        this.principalId = principalId;
        this.username = username;
        this.normalizedUsername = normalizedUsername;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.isActive = true;
    }

    public String getPrincipalId() { return principalId; }
    public void setPrincipalId(String principalId) { this.principalId = principalId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNormalizedUsername() { return normalizedUsername; }
    public void setNormalizedUsername(String normalizedUsername) { this.normalizedUsername = normalizedUsername; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Set<PrincipalRole> getRoles() { return roles; }
    public void setRoles(Set<PrincipalRole> roles) { this.roles = roles; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
