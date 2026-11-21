package vn.edu.fpt.cinemamanagement.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import vn.edu.fpt.cinemamanagement.entities.Booking;
import vn.edu.fpt.cinemamanagement.entities.BookingDetail;
import vn.edu.fpt.cinemamanagement.entities.ShowtimeSeat;
import vn.edu.fpt.cinemamanagement.enums.BookingStatus;
import vn.edu.fpt.cinemamanagement.enums.SeatStatus;
import vn.edu.fpt.cinemamanagement.repositories.BookingDetailRepository;
import vn.edu.fpt.cinemamanagement.repositories.BookingRepository;
import vn.edu.fpt.cinemamanagement.repositories.ShowtimeSeatRepository;
import vn.edu.fpt.cinemamanagement.repositories.VoucherRepository;

/** Releases the resources of an unpaid customer booking exactly once. */
@Service
public class CustomerBookingCancellationService {
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final VoucherRepository voucherRepository;

    public CustomerBookingCancellationService(
            BookingRepository bookingRepository,
            BookingDetailRepository bookingDetailRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            VoucherRepository voucherRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.voucherRepository = voucherRepository;
    }

    /**
     * Cancels only bookings that are still BOOKED at the time their row is locked.
     * This prevents the expiry scheduler from undoing a concurrently completed payment.
     */
    @Transactional
    public int cancelExpiredBookings(LocalDateTime expiresAt) {
        int cancelled = 0;
        List<String> bookingIds = bookingRepository.findIdsByStatusAndCreatedAtBefore(
                BookingStatus.BOOKED, expiresAt);
        for (String bookingId : bookingIds) {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
            if (booking != null
                    && BookingStatus.BOOKED.equals(booking.getStatus())
                    && booking.getCreatedAt() != null
                    && !booking.getCreatedAt().isAfter(expiresAt)) {
                cancelBookedBooking(booking);
                cancelled++;
            }
        }
        return cancelled;
    }

    /** Must run inside the current booking/payment transaction. */
    public void cancelBookedBooking(Booking booking) {
        if (!BookingStatus.BOOKED.equals(booking.getStatus())) {
            throw new IllegalStateException("Chỉ đơn đang chờ thanh toán mới có thể hủy.");
        }

        List<BookingDetail> details = bookingDetailRepository.findByBooking(booking);
        List<ShowtimeSeat> seatsToRelease = details.stream()
                .map(BookingDetail::getShowtimeSeat)
                .filter(seat -> seat != null && SeatStatus.PENDING.equals(seat.getStatus()))
                .toList();
        seatsToRelease.forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        showtimeSeatRepository.saveAll(seatsToRelease);

        restoreVoucherUse(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private void restoreVoucherUse(Booking booking) {
        if (booking.getVoucherId() == null || booking.getVoucherId().isBlank()) {
            return;
        }
        voucherRepository.findById(booking.getVoucherId()).ifPresent(voucher -> {
            int usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
            voucher.setUsedCount(Math.max(0, usedCount - 1));
            voucherRepository.save(voucher);
        });
    }
}
