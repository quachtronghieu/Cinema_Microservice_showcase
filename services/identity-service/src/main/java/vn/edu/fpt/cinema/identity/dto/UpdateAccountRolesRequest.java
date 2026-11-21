package vn.edu.fpt.cinema.identity.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UpdateAccountRolesRequest {

    @NotEmpty(message = "At least one role is required.")
    private List<String> roles;

    public UpdateAccountRolesRequest() {}

    public UpdateAccountRolesRequest(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
