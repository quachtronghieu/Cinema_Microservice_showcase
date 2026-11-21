package vn.edu.fpt.cinema.identity.dto;

public class ServiceTokenResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn = 300;
    private String scope;

    public ServiceTokenResponse() {}

    public ServiceTokenResponse(String accessToken, long expiresIn, String scope) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.scope = scope;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
