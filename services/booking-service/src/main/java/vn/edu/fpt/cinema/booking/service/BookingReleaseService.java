package vn.edu.fpt.cinema.booking.service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.repository.BookingRepository;
import vn.edu.fpt.cinema.booking.repository.ShowtimeSeatRepository;
import vn.edu.fpt.cinema.booking.repository.VoucherRepository;

/** Closes an unpaid booking (cancel, expiry, failed payment) and gives its seats and voucher use back exactly once. */
@Service
public class BookingReleaseService {
    private final BookingRepository bookings;
    private final ShowtimeSeatRepository seats;
    private final VoucherRepository vouchers;
    private final Clock clock;

    public BookingReleaseService(BookingRepository bookings, ShowtimeSeatRepository seats, VoucherRepository vouchers, Clock clock) {
        this.bookings = bookings;
        this.seats = seats;
        this.vouchers = vouchers;
        this.clock = clock;
    }

    /**
     * @param target CANCELLED or EXPIRED
     * @return true if this call closed the booking; false if it was no longer PENDING_PAYMENT (nothing released).
     */
    @Transactional
    public boolean release(String bookingId, String target) {
        var now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        // The conditional status update is the gate: concurrent cancel/expiry/payment cannot all pass it.
        if (bookings.closePending(bookingId, target, now) != 1) return false;
        seats.releaseBookingSeats(bookingId);
        vouchers.returnReservedUse(bookingId);
        vouchers.markReservationReleased(bookingId, now);
        return true;
    }
}
