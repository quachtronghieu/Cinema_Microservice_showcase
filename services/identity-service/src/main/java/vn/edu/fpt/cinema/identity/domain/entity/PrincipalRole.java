package vn.edu.fpt.cinema.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;

@Entity
@Table(name = "Principal_Role")
public class PrincipalRole {

    @Id
    @Column(name = "principal_role_id", length = 36)
    private String principalRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_id", nullable = false)
    private Principal principal;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 30)
    private RoleType roleName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PrincipalRole() {}

    public PrincipalRole(String principalRoleId, Principal principal, RoleType roleName) {
        this.principalRoleId = principalRoleId;
        this.principal = principal;
        this.roleName = roleName;
    }

    public String getPrincipalRoleId() { return principalRoleId; }
    public void setPrincipalRoleId(String principalRoleId) { this.principalRoleId = principalRoleId; }

    public Principal getPrincipal() { return principal; }
    public void setPrincipal(Principal principal) { this.principal = principal; }

    public RoleType getRoleName() { return roleName; }
    public void setRoleName(RoleType roleName) { this.roleName = roleName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
