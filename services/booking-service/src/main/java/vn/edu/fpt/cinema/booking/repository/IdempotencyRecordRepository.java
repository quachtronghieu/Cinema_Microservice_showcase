package vn.edu.fpt.cinema.booking.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.entity.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByActorIdAndOperationAndIdemKey(String actorId, String operation, String idemKey);

    @Transactional
    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);

    /** Frees one expired key so it can be reused instead of colliding with the unique constraint. */
    @Transactional
    @Modifying
    @Query("delete from IdempotencyRecord r where r.actorId = :actorId and r.operation = :operation "
        + "and r.idemKey = :idemKey and r.expiresAt <= :now")
    int deleteExpiredKey(@Param("actorId") String actorId, @Param("operation") String operation,
                         @Param("idemKey") String idemKey, @Param("now") Instant now);
}
