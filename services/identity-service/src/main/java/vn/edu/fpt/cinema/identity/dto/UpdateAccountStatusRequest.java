package vn.edu.fpt.cinema.identity.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateAccountStatusRequest {

    @NotNull(message = "isActive is required.")
    private Boolean isActive;

    private String reason;

    public UpdateAccountStatusRequest() {}

    public UpdateAccountStatusRequest(Boolean isActive, String reason) {
        this.isActive = isActive;
        this.reason = reason;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
