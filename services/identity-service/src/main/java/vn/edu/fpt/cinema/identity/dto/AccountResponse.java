package vn.edu.fpt.cinema.identity.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;

public class AccountResponse {

    private String principalId;
    private String username;
    private AccountType accountType;
    private String fullName;
    private String email;
    private String phone;
    private List<RoleType> roles;
    private Boolean isActive;
    private String domainId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountResponse() {}

    public AccountResponse(
            String principalId,
            String username,
            AccountType accountType,
            String fullName,
            String email,
            String phone,
            List<RoleType> roles,
            Boolean isActive,
            String domainId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.principalId = principalId;
        this.username = username;
        this.accountType = accountType;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roles = roles;
        this.isActive = isActive;
        this.domainId = domainId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPrincipalId() { return principalId; }
    public void setPrincipalId(String principalId) { this.principalId = principalId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<RoleType> getRoles() { return roles; }
    public void setRoles(List<RoleType> roles) { this.roles = roles; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
