package vn.edu.fpt.cinema.identity.dto;

import java.util.List;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn = 900;
    private String principalId;
    private String username;
    private String accountType;
    private List<String> roles;

    // Flutter client compatibility fields
    private String userId;
    private String role;

    public LoginResponse() {}

    public LoginResponse(
            String accessToken,
            long expiresIn,
            String principalId,
            String username,
            String accountType,
            List<String> roles,
            String domainUserId) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.principalId = principalId;
        this.username = username;
        this.accountType = accountType;
        this.roles = roles;
        this.userId = domainUserId != null ? domainUserId : principalId;
        this.role = (roles != null && !roles.isEmpty()) ? "ROLE_" + roles.get(0) : "ROLE_USER";
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getPrincipalId() { return principalId; }
    public void setPrincipalId(String principalId) { this.principalId = principalId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
