package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.fpt.cinema.booking.dto.BookingCreatedResponse;
import vn.edu.fpt.cinema.booking.dto.BookingStatusResponse;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.entity.*;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.*;

@Service
public class BookingService {
    /** Customer payment window, independent of the 5-minute hold (AGENTS 5.2). */
    static final Duration PAYMENT_WINDOW = Duration.ofMinutes(5);
    static final String OPERATION = "create-booking";

    private final PricingService pricing;
    private final IdempotencyService idempotency;
    private final BookingQueryService queries;
    private final BookingReleaseService release;
    private final SeatHoldRepository holds;
    private final ShowtimeSeatRepository seats;
    private final VoucherRepository vouchers;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public BookingService(PricingService pricing, IdempotencyService idempotency, BookingQueryService queries,
                          BookingReleaseService release, SeatHoldRepository holds,
                          ShowtimeSeatRepository seats, VoucherRepository vouchers, EntityManager entityManager,
                          TransactionTemplate transaction, Clock clock) {
        this.pricing = pricing;
        this.idempotency = idempotency;
        this.queries = queries;
        this.release = release;
        this.holds = holds;
        this.seats = seats;
        this.vouchers = vouchers;
        this.entityManager = entityManager;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** replayed = true when the same Idempotency-Key and payload already created this booking. */
    public record CreateResult(boolean replayed, BookingCreatedResponse body) {}

    public CreateResult create(String customerId, String idempotencyKey, CustomerBookingRequest request) {
        IdempotencyService.validateKey(idempotencyKey);
        String hash = RequestHasher.hash(OPERATION, request);
        var previous = idempotency.replay(customerId, OPERATION, idempotencyKey, hash, BookingCreatedResponse.class);
        if (previous.isPresent()) return new CreateResult(true, previous.get());
        try {
            // Price first, outside the transaction: the catalog HTTP call must not run while SQL locks are held.
            BookingQuote quote = pricing.quote(customerId, request);
            return new CreateResult(false, transaction.execute(status ->
                persist(customerId, idempotencyKey, hash, request.holdId(), quote)));
        } catch (BookingException | DataIntegrityViolationException e) {
            // A concurrent request with the same key may have committed first (its hold is now consumed,
            // or its idempotency row won the unique key): answer with that stored result instead.
            return idempotency.replay(customerId, OPERATION, idempotencyKey, hash, BookingCreatedResponse.class)
                .map(body -> new CreateResult(true, body)).orElseThrow(() -> e);
        }
    }

    /** Customer cancel: only an unpaid booking; repeating a cancel returns CANCELLED without releasing again. */
    public BookingStatusResponse cancel(String customerId, String bookingId) {
        queries.ownBooking(customerId, bookingId);
        if (release.release(bookingId, "CANCELLED")) return new BookingStatusResponse(bookingId, "CANCELLED");
        String status = queries.ownBooking(customerId, bookingId).getStatus();
        if ("CANCELLED".equals(status)) return new BookingStatusResponse(bookingId, status);
        throw new BookingException(CONFLICT, "BOOKING_NOT_CANCELLABLE", "Only an unpaid booking can be cancelled (status " + status + ").");
    }

    private BookingCreatedResponse persist(String customerId, String idempotencyKey, String requestHash,
                                           String holdId, BookingQuote quote) {
        SeatHold hold = holds.lockById(holdId)
            .orElseThrow(() -> new BookingException(NOT_FOUND, "HOLD_NOT_FOUND", "Seat hold not found."));
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        // Re-check under the lock: another request may have consumed or the hold may have expired since pricing.
        if (!hold.getCustomerId().equals(customerId)) {
            throw new BookingException(FORBIDDEN, "HOLD_NOT_OWNED", "This seat hold belongs to another customer.");
        }
        if (!hold.isUsableAt(now)) {
            throw new BookingException(CONFLICT, "HOLD_EXPIRED", "The seat hold has expired or was already used.");
        }

        String bookingId = UUID.randomUUID().toString();
        Voucher voucher = quote.voucher();
        Booking booking = Booking.pendingPayment(bookingId, customerId, hold.getShowtimeId(),
            voucher == null ? null : voucher.getVoucherId(), quote.subtotal(), quote.discountAmount(), quote.total(),
            now, now.plus(PAYMENT_WINDOW));
        entityManager.persist(booking);
        entityManager.flush(); // seats, hold and reservation reference the booking row

        List<String> seatIds = quote.seats().stream().map(ShowtimeSeat::getShowtimeSeatId).toList();
        if (seats.moveHeldSeatsToBooking(holdId, seatIds, bookingId) != seatIds.size()) {
            throw new BookingException(CONFLICT, "HOLD_SELECTION_MISMATCH", "The selected seats are no longer held by this hold.");
        }
        if (holds.consume(holdId, bookingId) != 1) {
            throw new BookingException(CONFLICT, "HOLD_EXPIRED", "The seat hold has expired or was already used.");
        }
        holds.releaseActiveSlot(holdId);

        quote.seats().forEach(seat -> entityManager.persist(BookingDetail.seat(bookingId, seat)));
        quote.concessionLines().forEach(line -> entityManager.persist(
            BookingDetail.concession(bookingId, line.concessionId(), line.name(), line.quantity(), line.unitPrice())));

        if (voucher != null) {
            if (vouchers.reserveOne(voucher.getVoucherId()) != 1) {
                throw new BookingException(CONFLICT, "VOUCHER_EXHAUSTED", "The voucher has no uses left.");
            }
            entityManager.persist(new VoucherReservation(bookingId, voucher.getVoucherId(), now));
        }
        var response = new BookingCreatedResponse(bookingId, booking.getStatus(), booking.getTotalAmount(), booking.getExpiresAt());
        idempotency.record(customerId, OPERATION, idempotencyKey, requestHash, bookingId, 201, response);
        return response;
    }
}
