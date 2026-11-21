package vn.edu.fpt.cinema.identity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.entity.PrincipalRole;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;

@Repository
public interface PrincipalRoleRepository extends JpaRepository<PrincipalRole, String> {
    List<PrincipalRole> findByPrincipal(Principal principal);
    boolean existsByPrincipalAndRoleName(Principal principal, RoleType roleName);
    long countByRoleName(RoleType roleName);
}
