package vn.edu.fpt.cinema.identity.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;

@Repository
public interface PrincipalRepository extends JpaRepository<Principal, String> {
    Optional<Principal> findByNormalizedUsername(String normalizedUsername);
    Optional<Principal> findByCustomerId(String customerId);
    Optional<Principal> findByStaffId(String staffId);
    boolean existsByNormalizedUsername(String normalizedUsername);
    long countByAccountType(AccountType accountType);

    @Query("SELECT p FROM Principal p WHERE " +
           "(:keyword IS NULL OR LOWER(p.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:accountType IS NULL OR p.accountType = :accountType) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<Principal> searchPrincipals(
            @Param("keyword") String keyword,
            @Param("accountType") AccountType accountType,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
