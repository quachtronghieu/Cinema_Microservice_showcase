package vn.edu.fpt.cinema.booking.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.booking.entity.ShowtimeSeat;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, String> {
    /** Current state read under the row lock, so it cannot be a stale copy from the persistence context. */
    interface LockedSeat {
        String getStatus();
        String getHoldId();
    }

    List<ShowtimeSeat> findByHoldId(String holdId);

    List<ShowtimeSeat> findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(String showtimeId);

    @Query(value = "SELECT status AS status, hold_id AS holdId FROM dbo.Showtime_Seat WITH (UPDLOCK, ROWLOCK) "
        + "WHERE showtime_seat_id = CAST(:seatId AS VARCHAR(36)) AND showtime_id = CAST(:showtimeId AS VARCHAR(36))", nativeQuery = true)
    Optional<LockedSeat> lockSeat(@Param("showtimeId") String showtimeId, @Param("seatId") String seatId);

    @Query(value = "SELECT showtime_seat_id FROM dbo.Showtime_Seat WHERE hold_id = CAST(:holdId AS VARCHAR(36))", nativeQuery = true)
    List<String> seatIdsOfHold(@Param("holdId") String holdId);

    /** The check on AVAILABLE is what stops two holds from owning one seat; callers compare the row count. */
    @Modifying
    @Query(value = "UPDATE dbo.Showtime_Seat SET status = 'HELD', hold_id = CAST(:holdId AS VARCHAR(36)), version = version + 1 "
        + "WHERE showtime_seat_id = CAST(:seatId AS VARCHAR(36)) AND showtime_id = CAST(:showtimeId AS VARCHAR(36)) AND status = 'AVAILABLE'", nativeQuery = true)
    int holdSeat(@Param("showtimeId") String showtimeId, @Param("seatId") String seatId, @Param("holdId") String holdId);

    /** Frees only seats still held by this hold; seats moved to a booking no longer carry its ID. */
    @Modifying
    @Query(value = "UPDATE dbo.Showtime_Seat SET status = 'AVAILABLE', hold_id = NULL, version = version + 1 "
        + "WHERE hold_id = CAST(:holdId AS VARCHAR(36)) AND status = 'HELD'", nativeQuery = true)
    int releaseHoldSeats(@Param("holdId") String holdId);

    @Modifying
    @Query(value = "UPDATE dbo.Showtime_Seat SET status = 'SOLD', version = version + 1 "
        + "WHERE booking_id = :bookingId AND status = 'HELD'", nativeQuery = true)
    int sellBookingSeats(@Param("bookingId") String bookingId);

    /** Gives a closed booking's seats back to sale; SOLD seats are never touched. */
    @Modifying
    @Query(value = "UPDATE dbo.Showtime_Seat SET status = 'AVAILABLE', booking_id = NULL, version = version + 1 "
        + "WHERE booking_id = :bookingId AND status = 'HELD'", nativeQuery = true)
    int releaseBookingSeats(@Param("bookingId") String bookingId);

    /** Moves seat ownership from the hold to the booking; callers compare the row count with the seat count. */
    @Modifying
    @Query(value = "UPDATE dbo.Showtime_Seat SET booking_id = :bookingId, hold_id = NULL, version = version + 1 "
        + "WHERE hold_id = :holdId AND status = 'HELD' AND showtime_seat_id IN (:seatIds)", nativeQuery = true)
    int moveHeldSeatsToBooking(@Param("holdId") String holdId, @Param("seatIds") Collection<String> seatIds,
                               @Param("bookingId") String bookingId);
}
