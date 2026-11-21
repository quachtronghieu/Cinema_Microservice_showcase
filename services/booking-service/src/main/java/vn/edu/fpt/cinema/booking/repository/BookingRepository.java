package vn.edu.fpt.cinema.booking.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {
    Page<Booking> findByCustomerIdAndStatus(String customerId, String status, Pageable pageable);

    /** Only a PENDING_PAYMENT booking can be closed; 0 rows means another command already decided it. */
    @Modifying
    @Query(value = "UPDATE dbo.Booking SET status = :target, updated_at = :now, version = version + 1 "
        + "WHERE booking_id = :bookingId AND status = 'PENDING_PAYMENT'", nativeQuery = true)
    int closePending(@Param("bookingId") String bookingId, @Param("target") String target, @Param("now") Instant now);

    /** Pays only an unpaid booking still inside its payment window; 0 rows = cancelled, expired or already paid. */
    @Modifying
    @Query(value = "UPDATE dbo.Booking SET status = 'PAID', updated_at = :now, version = version + 1 "
        + "WHERE booking_id = :bookingId AND status = 'PENDING_PAYMENT' AND expires_at > :now", nativeQuery = true)
    int markPaid(@Param("bookingId") String bookingId, @Param("now") Instant now);

    @Query("select b.bookingId from Booking b where b.status = 'PENDING_PAYMENT' and b.expiresAt <= :now order by b.expiresAt")
    List<String> findOverdueIds(@Param("now") Instant now, Pageable pageable);
}
