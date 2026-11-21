package vn.edu.fpt.cinema.identity.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.identity.domain.entity.PasswordResetToken;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    Optional<PasswordResetToken> findTopByPrincipalOrderByExpiresAtDesc(Principal principal);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
