package vn.edu.fpt.cinema.booking.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.booking.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, String> {
    /** booking_db uses a case-insensitive collation, so codes match regardless of case. */
    Optional<Voucher> findByCode(String code);

    /** On payment the reserved use becomes a used one. */
    @Modifying
    @Query(value = "UPDATE dbo.Voucher SET reserved_count = reserved_count - 1, used_count = used_count + 1, version = version + 1 "
        + "WHERE voucher_id = (SELECT voucher_id FROM dbo.Voucher_Reservation WHERE booking_id = :bookingId AND status = 'RESERVED')",
        nativeQuery = true)
    int consumeReservedUse(@Param("bookingId") String bookingId);

    @Modifying
    @Query(value = "UPDATE dbo.Voucher_Reservation SET status = 'CONSUMED', updated_at = :now "
        + "WHERE booking_id = :bookingId AND status = 'RESERVED'", nativeQuery = true)
    int markReservationConsumed(@Param("bookingId") String bookingId, @Param("now") java.time.Instant now);

    /** Returns the use reserved by this booking; the RESERVED reservation row makes it happen at most once. */
    @Modifying
    @Query(value = "UPDATE dbo.Voucher SET reserved_count = reserved_count - 1, version = version + 1 "
        + "WHERE voucher_id = (SELECT voucher_id FROM dbo.Voucher_Reservation WHERE booking_id = :bookingId AND status = 'RESERVED')",
        nativeQuery = true)
    int returnReservedUse(@Param("bookingId") String bookingId);

    @Modifying
    @Query(value = "UPDATE dbo.Voucher_Reservation SET status = 'RELEASED', updated_at = :now "
        + "WHERE booking_id = :bookingId AND status = 'RESERVED'", nativeQuery = true)
    int markReservationReleased(@Param("bookingId") String bookingId, @Param("now") java.time.Instant now);

    /** Compare-and-set on the quota: returns 0 when the last use was taken by someone else first. */
    @Modifying
    @Query(value = "UPDATE dbo.Voucher SET reserved_count = reserved_count + 1, version = version + 1 "
        + "WHERE voucher_id = :voucherId AND status = 1 AND used_count + reserved_count < usage_limit", nativeQuery = true)
    int reserveOne(@Param("voucherId") String voucherId);
}
