package vn.edu.fpt.cinema.catalog.concession.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.catalog.concession.entity.Concession;
import java.util.Optional;

public interface ConcessionRepository extends JpaRepository<Concession,String>, JpaSpecificationExecutor<Concession> {
    // PUT, DELETE and no-op version checks serialize on the same database row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Concession c where c.concessionId = :id")
    Optional<Concession> findForUpdate(@Param("id") String id);
}
