package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import feign.FeignException;
import java.time.Clock;
import java.util.*;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.entity.*;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.service.BookingQuote.ConcessionLine;
import vn.edu.fpt.cinema.booking.repository.*;

/** Prices a customer's active hold. Read-only: never changes the hold, seats or voucher quota. */
@Service
public class PricingService {
    private final SeatHoldRepository holds;
    private final ShowtimeSeatRepository seats;
    private final SalesSnapshotRepository showtimes;
    private final VoucherRepository vouchers;
    private final CatalogClient catalog;
    private final Clock clock;

    public PricingService(SeatHoldRepository holds, ShowtimeSeatRepository seats, SalesSnapshotRepository showtimes,
                          VoucherRepository vouchers, CatalogClient catalog, Clock clock) {
        this.holds = holds;
        this.seats = seats;
        this.showtimes = showtimes;
        this.vouchers = vouchers;
        this.catalog = catalog;
        this.clock = clock;
    }

    public BookingQuote quote(String customerId, CustomerBookingRequest request) {
        var requestedSeats = new LinkedHashSet<>(request.showtimeSeatIds());
        if (requestedSeats.size() != request.showtimeSeatIds().size()) {
            throw new BookingException(BAD_REQUEST, "INVALID_SEAT_SELECTION", "The same seat appears more than once.");
        }
        SeatHold hold = holds.findById(request.holdId())
            .orElseThrow(() -> new BookingException(NOT_FOUND, "HOLD_NOT_FOUND", "Seat hold not found."));
        if (!hold.getCustomerId().equals(customerId)) {
            throw new BookingException(FORBIDDEN, "HOLD_NOT_OWNED", "This seat hold belongs to another customer.");
        }
        if (!hold.getShowtimeId().equals(request.showtimeId())) {
            throw mismatch();
        }
        if (!hold.isUsableAt(clock.instant())) {
            throw new BookingException(CONFLICT, "HOLD_EXPIRED", "The seat hold has expired. Please select seats again.");
        }
        showtimes.findById(hold.getShowtimeId()).filter(SalesSnapshot::isOpen)
            .orElseThrow(() -> new BookingException(CONFLICT, "SALES_CLOSED", "Ticket sales for this showtime are closed."));

        Map<String, ShowtimeSeat> held = new HashMap<>();
        seats.findByHoldId(hold.getHoldId()).stream().filter(seat -> "HELD".equals(seat.getStatus()))
            .forEach(seat -> held.put(seat.getShowtimeSeatId(), seat));
        if (!held.keySet().equals(requestedSeats)) {
            throw mismatch();
        }
        List<ShowtimeSeat> pricedSeats = requestedSeats.stream().map(held::get).toList();
        return BookingQuote.of(pricedSeats, concessionLines(request), voucher(request.voucherCode()));
    }

    private List<ConcessionLine> concessionLines(CustomerBookingRequest request) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        request.concessionsOrEmpty().forEach(item -> quantities.merge(item.concessionId().trim(), item.quantity(), Integer::sum));
        String bearer = bearer();
        return quantities.entrySet().stream().map(entry -> {
            CatalogClient.Concession concession = concession(bearer, entry.getKey());
            return new ConcessionLine(concession.concessionId(), concession.name(), concession.price(), entry.getValue());
        }).toList();
    }

    private CatalogClient.Concession concession(String bearer, String concessionId) {
        CatalogClient.Concession concession;
        try {
            concession = catalog.concession(bearer, concessionId);
        } catch (RuntimeException e) {
            Throwable cause = e instanceof NoFallbackAvailableException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof FeignException.NotFound) throw concessionUnavailable(concessionId);
            // Never price with 0 or a stale value when catalog cannot answer (AGENTS 8).
            throw new BookingException(SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", "The concession catalog is temporarily unavailable.");
        }
        if (concession == null || !concession.active() || concession.price() == null || concession.price().signum() <= 0) {
            throw concessionUnavailable(concessionId);
        }
        return concession;
    }

    private Voucher voucher(String code) {
        if (code == null || code.isBlank()) return null;
        return vouchers.findByCode(code.trim()).filter(Voucher::hasQuotaLeft)
            .orElseThrow(() -> new BookingException(CONFLICT, "VOUCHER_UNAVAILABLE", "The voucher is invalid or has no uses left."));
    }

    private static String bearer() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token
            ? "Bearer " + token.getToken().getTokenValue() : null;
    }

    private static BookingException mismatch() {
        return new BookingException(CONFLICT, "HOLD_SELECTION_MISMATCH", "The selected seats do not match the seat hold.");
    }

    private static BookingException concessionUnavailable(String concessionId) {
        return new BookingException(CONFLICT, "CONCESSION_UNAVAILABLE", "Concession " + concessionId + " is no longer available.");
    }
}
