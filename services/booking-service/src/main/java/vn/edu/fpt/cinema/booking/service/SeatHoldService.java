package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.fpt.cinema.booking.dto.SeatAvailabilityResponse;
import vn.edu.fpt.cinema.booking.dto.SeatHoldRequest;
import vn.edu.fpt.cinema.booking.dto.SeatHoldResponse;
import vn.edu.fpt.cinema.booking.entity.SalesSnapshot;
import vn.edu.fpt.cinema.booking.entity.SeatHold;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.exception.SeatUnavailableException;
import vn.edu.fpt.cinema.booking.repository.SalesSnapshotRepository;
import vn.edu.fpt.cinema.booking.repository.SeatHoldRepository;
import vn.edu.fpt.cinema.booking.repository.ShowtimeSeatRepository;

/**
 * Holds and releases seats. All state is in booking_db, so any instance can serve any request (AGENTS 5.1).
 * Lock order everywhere: ActiveHold slot, then SeatHold, then seats in sorted ID order.
 * Events are logged with IDs only, as message text and as structured fields, so one hold can be followed in the central logs.
 */
@Service
public class SeatHoldService {
    private static final Logger LOG = LoggerFactory.getLogger(SeatHoldService.class);
    static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final SalesSnapshotRepository showtimes;
    private final SeatHoldRepository holds;
    private final ShowtimeSeatRepository seats;
    private final EntityManager entityManager;
    private final Clock clock;

    public SeatHoldService(SalesSnapshotRepository showtimes, SeatHoldRepository holds, ShowtimeSeatRepository seats,
                           EntityManager entityManager, Clock clock) {
        this.showtimes = showtimes;
        this.holds = holds;
        this.seats = seats;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /** All seats or none; a failed replacement rolls back and leaves the customer's previous hold in place. */
    @Transactional
    public SeatHoldResponse hold(String customerId, SeatHoldRequest request) {
        String showtimeId = request.showtimeId().trim();
        try {
            return holdSeats(customerId, showtimeId, request);
        } catch (BookingException e) {
            List<String> lost = e instanceof SeatUnavailableException unavailable ? unavailable.seatIds() : List.of();
            event("SEAT_HOLD_REJECTED", null, showtimeId, customerId, lost, e.code());
            throw e;
        }
    }

    private SeatHoldResponse holdSeats(String customerId, String showtimeId, SeatHoldRequest request) {
        List<String> requested = distinctSeatIds(request.showtimeSeatIds());
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        checkOnSale(showtimeId, now);

        // Serializes this customer's holds for the showtime, including when no slot row exists yet.
        Optional<String> previousHold = holds.lockActiveSlot(customerId, showtimeId);
        previousHold.ifPresent(holds::lockStatus);

        // Lock old and new seats together in one sorted order, so two customers swapping seats cannot deadlock.
        Set<String> toLock = new TreeSet<>(requested);
        previousHold.ifPresent(holdId -> toLock.addAll(seats.seatIdsOfHold(holdId)));
        Map<String, ShowtimeSeatRepository.LockedSeat> locked = new HashMap<>();
        for (String seatId : toLock) {
            seats.lockSeat(showtimeId, seatId).ifPresent(seat -> locked.put(seatId, seat));
        }
        checkRequestedSeats(requested, locked, previousHold.orElse(null));

        previousHold.ifPresent(this::releaseActive);
        String holdId = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(HOLD_DURATION);
        entityManager.persist(SeatHold.active(holdId, customerId, showtimeId, now, expiresAt));
        entityManager.flush(); // seats, items and the slot reference this row
        for (String seatId : requested) {
            if (seats.holdSeat(showtimeId, seatId, holdId) != 1) throw new SeatUnavailableException(List.of(seatId));
            holds.insertItem(holdId, seatId);
        }
        holds.insertActiveSlot(customerId, showtimeId, holdId);
        afterCommit(() -> event(previousHold.isPresent() ? "SEAT_HOLD_REPLACED" : "SEAT_HOLD_CREATED", holdId, showtimeId, customerId, requested, null));
        return new SeatHoldResponse(holdId, showtimeId, requested, expiresAt, now);
    }

    /** Repeating a release is harmless; seats that already became a booking are never touched. */
    @Transactional
    public void release(String customerId, String holdId) {
        SeatHold hold = holds.findById(holdId)
            .orElseThrow(() -> new BookingException(NOT_FOUND, "HOLD_NOT_FOUND", "Không tìm thấy lượt giữ ghế."));
        if (!hold.getCustomerId().equals(customerId)) {
            throw new BookingException(FORBIDDEN, "HOLD_NOT_OWNED", "Lượt giữ ghế này thuộc về khách khác.");
        }
        holds.lockActiveSlot(customerId, hold.getShowtimeId());
        String status = holds.lockStatus(holdId);
        if ("CONSUMED".equals(status)) {
            throw new BookingException(CONFLICT, "HOLD_ALREADY_CONSUMED", "Ghế đã được chuyển vào đơn đặt vé.");
        }
        if ("ACTIVE".equals(status)) {
            releaseActive(holdId);
            afterCommit(() -> event("SEAT_HOLD_RELEASED", holdId, hold.getShowtimeId(), customerId, List.of(), null));
        }
    }

    /** Called by the expiry job; the conditional update lets one winner close the hold when a booking competes for it. */
    @Transactional
    public boolean expire(String holdId) {
        holds.releaseActiveSlot(holdId);
        if (holds.close(holdId, "EXPIRED") != 1) return false;
        seats.releaseHoldSeats(holdId);
        String showtimeId = holds.findById(holdId).map(SeatHold::getShowtimeId).orElse(null);
        afterCommit(() -> event("SEAT_HOLD_EXPIRED", holdId, showtimeId, null, List.of(), null));
        return true;
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse availability(String showtimeId) {
        SalesSnapshot showtime = showtimes.findById(showtimeId).orElseThrow(SeatHoldService::showtimeNotFound);
        var items = seats.findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(showtimeId).stream()
            .map(seat -> new SeatAvailabilityResponse.Seat(seat.getShowtimeSeatId(), seat.getTemplateSeatId(), seat.getRowLabel(),
                seat.getSeatNumber(), seat.getSeatType(), seat.getStatus(), seat.getUnitPrice()))
            .toList();
        return new SeatAvailabilityResponse(showtime.getShowtimeId(), showtime.getSalesStatus(),
            clock.instant().truncatedTo(ChronoUnit.MILLIS), items);
    }

    /** A success is logged only once it is committed, so a rolled-back hold never appears as created. */
    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private static void event(String event, String holdId, String showtimeId, String customerId, List<String> seatIds, String reason) {
        LoggingEventBuilder log = LOG.atInfo().addKeyValue("event.action", event);
        if (holdId != null) log = log.addKeyValue("holdId", holdId);
        if (showtimeId != null) log = log.addKeyValue("showtimeId", showtimeId);
        if (customerId != null) log = log.addKeyValue("customerId", customerId);
        if (!seatIds.isEmpty()) log = log.addKeyValue("seatIds", String.join(",", seatIds));
        if (reason != null) log = log.addKeyValue("reason", reason);
        log.log("{} holdId={} showtimeId={} customerId={} seatIds={} reason={}", event, holdId, showtimeId, customerId, seatIds, reason);
    }

    private void releaseActive(String holdId) {
        holds.close(holdId, "RELEASED");
        seats.releaseHoldSeats(holdId);
        holds.releaseActiveSlot(holdId);
    }

    /** A shared lock on the row makes a future close-sales wait for holds in progress (AGENTS 4). */
    private void checkOnSale(String showtimeId, Instant now) {
        SalesSnapshot showtime = showtimes.lockForHold(showtimeId).orElseThrow(SeatHoldService::showtimeNotFound);
        if (!showtime.isOpen()) {
            throw new BookingException(CONFLICT, "SALES_CLOSED", "Suất chiếu đã đóng bán.");
        }
        LocalDateTime start = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
        if (!now.isBefore(start.atZone(BookingQueryService.CINEMA_ZONE).toInstant())) {
            throw new BookingException(CONFLICT, "SHOWTIME_STARTED", "Suất chiếu đã bắt đầu.");
        }
    }

    private static void checkRequestedSeats(List<String> requested, Map<String, ShowtimeSeatRepository.LockedSeat> locked,
                                            String previousHold) {
        if (!locked.keySet().containsAll(requested)) {
            throw new BookingException(BAD_REQUEST, "INVALID_SEATS", "Một số ghế không thuộc suất chiếu này.");
        }
        List<String> unavailable = requested.stream().filter(seatId -> {
            var seat = locked.get(seatId);
            boolean mine = "HELD".equals(seat.getStatus()) && previousHold != null && previousHold.equals(seat.getHoldId());
            return !"AVAILABLE".equals(seat.getStatus()) && !mine;
        }).toList();
        if (!unavailable.isEmpty()) throw new SeatUnavailableException(unavailable);
    }

    private static List<String> distinctSeatIds(List<String> seatIds) {
        var distinct = new TreeSet<String>();
        seatIds.forEach(seatId -> distinct.add(seatId.trim()));
        if (distinct.size() != seatIds.size()) {
            throw new BookingException(BAD_REQUEST, "INVALID_SEAT_SELECTION", "Một ghế được chọn nhiều lần.");
        }
        return List.copyOf(distinct);
    }

    private static BookingException showtimeNotFound() {
        return new BookingException(NOT_FOUND, "SHOWTIME_NOT_FOUND", "Không tìm thấy suất chiếu đang bán.");
    }
}
