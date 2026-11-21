package vn.edu.fpt.cinema.booking.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.booking.entity.SeatHold;

public interface SeatHoldRepository extends JpaRepository<SeatHold, String> {
    /** UPDLOCK on the hold row: concurrent attempts to consume the same hold are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from SeatHold h where h.holdId = :holdId")
    Optional<SeatHold> lockById(@Param("holdId") String holdId);

    @Modifying
    @Query(value = "UPDATE dbo.SeatHold SET status = 'CONSUMED', booking_id = :bookingId, version = version + 1 "
        + "WHERE hold_id = :holdId AND status = 'ACTIVE'", nativeQuery = true)
    int consume(@Param("holdId") String holdId, @Param("bookingId") String bookingId);

    @Modifying
    @Query(value = "DELETE FROM dbo.ActiveHold WHERE hold_id = :holdId", nativeQuery = true)
    int releaseActiveSlot(@Param("holdId") String holdId);

    // Native queries below CAST their parameters: the driver sends NVARCHAR by default, which can make SQL Server scan
    // and lock far more rows than the one requested on these VARCHAR keys.

    /** Key-range lock: serializes one customer's holds for a showtime even while no slot row exists. */
    @Query(value = "SELECT hold_id FROM dbo.ActiveHold WITH (UPDLOCK, HOLDLOCK) "
        + "WHERE customer_id = CAST(:customerId AS VARCHAR(36)) AND showtime_id = CAST(:showtimeId AS VARCHAR(36))", nativeQuery = true)
    Optional<String> lockActiveSlot(@Param("customerId") String customerId, @Param("showtimeId") String showtimeId);

    @Query(value = "SELECT status FROM dbo.SeatHold WITH (UPDLOCK, ROWLOCK) WHERE hold_id = CAST(:holdId AS VARCHAR(36))", nativeQuery = true)
    String lockStatus(@Param("holdId") String holdId);

    /** Only an ACTIVE hold can close, so a consumed hold can never be released or expired. */
    @Modifying
    @Query(value = "UPDATE dbo.SeatHold SET status = CAST(:status AS VARCHAR(10)), version = version + 1 "
        + "WHERE hold_id = CAST(:holdId AS VARCHAR(36)) AND status = 'ACTIVE'", nativeQuery = true)
    int close(@Param("holdId") String holdId, @Param("status") String status);

    @Modifying
    @Query(value = "INSERT INTO dbo.SeatHold_Item (hold_id, showtime_seat_id) VALUES (CAST(:holdId AS VARCHAR(36)), CAST(:seatId AS VARCHAR(36)))", nativeQuery = true)
    int insertItem(@Param("holdId") String holdId, @Param("seatId") String seatId);

    @Modifying
    @Query(value = "INSERT INTO dbo.ActiveHold (customer_id, showtime_id, hold_id) "
        + "VALUES (CAST(:customerId AS VARCHAR(36)), CAST(:showtimeId AS VARCHAR(36)), CAST(:holdId AS VARCHAR(36)))", nativeQuery = true)
    int insertActiveSlot(@Param("customerId") String customerId, @Param("showtimeId") String showtimeId, @Param("holdId") String holdId);

    @Query("select h.holdId from SeatHold h where h.status = 'ACTIVE' and h.expiresAt <= :now order by h.expiresAt")
    List<String> findExpiredIds(@Param("now") Instant now, Pageable page);
}
