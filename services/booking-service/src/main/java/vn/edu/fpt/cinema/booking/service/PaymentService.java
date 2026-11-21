package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.CONFLICT;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.fpt.cinema.booking.dto.DemoPaymentResponse;
import vn.edu.fpt.cinema.booking.entity.Booking;
import vn.edu.fpt.cinema.booking.entity.Payment;
import vn.edu.fpt.cinema.booking.entity.Ticket;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.*;

/**
 * Demo payment (no real bank). Retry-safe per booking: Payment.booking_id is unique, so the same outcome sent again
 * returns the stored payment and ticket, and a different outcome is rejected.
 */
@Service
public class PaymentService {
    private final BookingQueryService queries;
    private final BookingReleaseService release;
    private final BookingRepository bookings;
    private final ShowtimeSeatRepository seats;
    private final VoucherRepository vouchers;
    private final PaymentRepository payments;
    private final TicketRepository tickets;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public PaymentService(BookingQueryService queries, BookingReleaseService release, BookingRepository bookings,
                          ShowtimeSeatRepository seats, VoucherRepository vouchers, PaymentRepository payments,
                          TicketRepository tickets, TransactionTemplate transaction, Clock clock) {
        this.queries = queries;
        this.release = release;
        this.bookings = bookings;
        this.seats = seats;
        this.vouchers = vouchers;
        this.payments = payments;
        this.tickets = tickets;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** @param outcome SUCCESS or FAILED */
    public DemoPaymentResponse pay(String customerId, String bookingId, String outcome) {
        Booking booking = queries.ownBooking(customerId, bookingId);
        var previous = replay(bookingId, outcome);
        if (previous.isPresent()) return previous.get();
        try {
            return transaction.execute(status -> "SUCCESS".equals(outcome) ? succeed(booking) : fail(booking));
        } catch (BookingException | DataIntegrityViolationException e) {
            // A concurrent identical request may have won (double click): answer with its stored result.
            return replay(bookingId, outcome).orElseThrow(() -> e);
        }
    }

    private DemoPaymentResponse succeed(Booking booking) {
        var now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        String bookingId = booking.getBookingId();
        // Same gate as cancel/expiry: only one of them can move the booking out of PENDING_PAYMENT.
        if (bookings.markPaid(bookingId, now) != 1) throw notPayable();
        seats.sellBookingSeats(bookingId);
        vouchers.consumeReservedUse(bookingId);
        vouchers.markReservationConsumed(bookingId, now);
        Payment payment = payments.saveAndFlush(Payment.demo(bookingId, "SUCCEEDED", booking.getTotalAmount(), now));
        Ticket ticket = tickets.saveAndFlush(new Ticket(bookingId, now));
        return response(payment, ticket.getTicketId());
    }

    private DemoPaymentResponse fail(Booking booking) {
        var now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        // Joins this transaction: the release and the FAILED payment row commit together.
        if (!release.release(booking.getBookingId(), "CANCELLED")) throw notPayable();
        Payment payment = payments.saveAndFlush(Payment.demo(booking.getBookingId(), "FAILED", booking.getTotalAmount(), now));
        return response(payment, null);
    }

    private Optional<DemoPaymentResponse> replay(String bookingId, String outcome) {
        return payments.findByBookingId(bookingId).map(payment -> {
            if (!payment.getStatus().equals("SUCCESS".equals(outcome) ? "SUCCEEDED" : "FAILED")) throw notPayable();
            return response(payment, tickets.findByBookingId(bookingId).map(Ticket::getTicketId).orElse(null));
        });
    }

    private static DemoPaymentResponse response(Payment payment, String ticketId) {
        String message = "SUCCEEDED".equals(payment.getStatus())
            ? "Thanh toán thành công. Vé đã được phát hành."
            : "Thanh toán thất bại. Đơn đã bị huỷ và ghế đã được trả lại.";
        return new DemoPaymentResponse(payment.getBookingId(), payment.getPaymentId(), payment.getStatus(),
            payment.getAmount(), ticketId, message);
    }

    private static BookingException notPayable() {
        return new BookingException(CONFLICT, "BOOKING_NOT_PAYABLE",
            "This booking can no longer be paid (already paid, cancelled or past its payment deadline). Please book again.");
    }
}
