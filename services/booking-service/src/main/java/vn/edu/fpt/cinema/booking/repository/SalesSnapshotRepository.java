package vn.edu.fpt.cinema.booking.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.booking.entity.SalesSnapshot;

public interface SalesSnapshotRepository extends JpaRepository<SalesSnapshot, String> {
    /** Shared lock: holds do not block each other, but a later close-sales waits for those in progress. */
    @Query(value = "SELECT * FROM dbo.Sales_Snapshot WITH (HOLDLOCK, ROWLOCK) WHERE showtime_id = CAST(:showtimeId AS VARCHAR(36))", nativeQuery = true)
    Optional<SalesSnapshot> lockForHold(@Param("showtimeId") String showtimeId);

    /** Waits for holds in progress (they share-lock the row); afterwards no new hold or booking can start. */
    @Modifying
    @Query(value = "UPDATE dbo.Sales_Snapshot SET sales_status = 'CLOSED', snapshot_version = snapshot_version + 1 "
        + "WHERE showtime_id = :showtimeId AND sales_status = 'OPEN'", nativeQuery = true)
    int close(@Param("showtimeId") String showtimeId);
}
