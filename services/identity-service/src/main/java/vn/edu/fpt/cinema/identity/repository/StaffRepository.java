package vn.edu.fpt.cinema.identity.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.identity.domain.entity.Staff;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    Optional<Staff> findByUsername(String username);
    Optional<Staff> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
