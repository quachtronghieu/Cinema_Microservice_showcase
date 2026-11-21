package vn.edu.fpt.cinema.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    private String token;

    // Legacy fields for backward compatibility with React/Flutter
    private String id;
    private Long resetTimestamp;
    private String resetCode;

    @NotBlank(message = "New password is required.")
    @Size(min = 6, message = "The password must be at least 6 characters long.")
    private String newPassword;

    @NotBlank(message = "Confirmation password is required.")
    private String confirmPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public ResetPasswordRequest(String id, Long resetTimestamp, String resetCode, String newPassword, String confirmPassword) {
        this.id = id;
        this.resetTimestamp = resetTimestamp;
        this.resetCode = resetCode;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getToken() {
        if (token != null && !token.isBlank()) {
            return token;
        }
        return resetCode;
    }

    public void setToken(String token) { this.token = token; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getResetTimestamp() { return resetTimestamp; }
    public void setResetTimestamp(Long resetTimestamp) { this.resetTimestamp = resetTimestamp; }

    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
