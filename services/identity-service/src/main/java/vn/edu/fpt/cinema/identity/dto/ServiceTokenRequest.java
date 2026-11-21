package vn.edu.fpt.cinema.identity.dto;

import jakarta.validation.constraints.NotBlank;

public class ServiceTokenRequest {

    @NotBlank(message = "Client ID cannot be blank")
    private String clientId;

    @NotBlank(message = "Client Secret cannot be blank")
    private String clientSecret;

    @NotBlank(message = "Target service cannot be blank")
    private String targetService;

    @NotBlank(message = "Scope cannot be blank")
    private String scope;

    public ServiceTokenRequest() {}

    public ServiceTokenRequest(String clientId, String clientSecret, String targetService, String scope) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.targetService = targetService;
        this.scope = scope;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getTargetService() { return targetService; }
    public void setTargetService(String targetService) { this.targetService = targetService; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
