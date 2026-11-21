package vn.edu.fpt.cinema.booking.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.cinema.booking.entity.BookingDetail;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, String> {
    List<BookingDetail> findByBookingIdOrderByItemName(String bookingId);

    /** Seat lines of a whole page of bookings in one query. */
    List<BookingDetail> findByBookingIdInAndItemType(Collection<String> bookingIds, String itemType);
}
