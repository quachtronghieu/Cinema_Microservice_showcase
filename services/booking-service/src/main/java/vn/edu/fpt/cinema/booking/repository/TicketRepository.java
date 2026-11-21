package vn.edu.fpt.cinema.booking.repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.cinema.booking.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    Optional<Ticket> findByBookingId(String bookingId);

    /** Tickets exist only for PAID bookings (issued in the payment transaction; PAID is terminal). */
    @Query("SELECT t FROM Ticket t JOIN Booking b ON b.bookingId = t.bookingId WHERE b.customerId = :customerId ORDER BY t.issuedAt DESC")
    List<Ticket> findAllOfCustomer(@Param("customerId") String customerId);

    /** Single-use gate: only a not-yet-used ticket of a PAID booking is updated; 0 rows means someone else won. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        UPDATE t SET checked_in = 1, checked_in_at = :now, checked_in_by = :staffId
        FROM dbo.Ticket t JOIN dbo.Booking b ON b.booking_id = t.booking_id
        WHERE t.ticket_id = :ticketId AND t.checked_in = 0 AND b.status = 'PAID'""")
    int checkIn(@Param("ticketId") String ticketId, @Param("staffId") String staffId, @Param("now") Instant now);
}
