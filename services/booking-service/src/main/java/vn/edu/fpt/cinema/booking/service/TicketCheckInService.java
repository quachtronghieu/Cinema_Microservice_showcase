package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.dto.CheckInResponse;
import vn.edu.fpt.cinema.booking.entity.Booking;
import vn.edu.fpt.cinema.booking.entity.Ticket;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.BookingRepository;
import vn.edu.fpt.cinema.booking.repository.SalesSnapshotRepository;
import vn.edu.fpt.cinema.booking.repository.TicketRepository;

/** ADMIN check-in at the door: each ticket can be used once, before its showtime starts. */
@Service
public class TicketCheckInService {
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final SalesSnapshotRepository showtimes;
    private final BookingQueryService queries;
    private final Clock clock;

    public TicketCheckInService(TicketRepository tickets, BookingRepository bookings, SalesSnapshotRepository showtimes,
                                BookingQueryService queries, Clock clock) {
        this.tickets = tickets;
        this.bookings = bookings;
        this.showtimes = showtimes;
        this.queries = queries;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CheckInResponse checkIn(String ticketId, String staffId) {
        Ticket ticket = tickets.findById(ticketId)
            .orElseThrow(() -> new BookingException(NOT_FOUND, "TICKET_NOT_FOUND", "Ticket not found."));
        if (ticket.isCheckedIn()) throw alreadyCheckedIn();
        Booking booking = bookings.findById(ticket.getBookingId()).orElseThrow();
        if ("EXPIRED".equals(queries.ticketStatus(ticket, showtimes.findById(booking.getShowtimeId()).orElseThrow()))) {
            throw new BookingException(CONFLICT, "TICKET_EXPIRED", "This showtime has already started; the ticket can no longer be used.");
        }
        var now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        if (tickets.checkIn(ticketId, staffId, now) != 1) {
            // Lost to a concurrent check-in, or the booking is no longer PAID.
            if (tickets.findById(ticketId).map(Ticket::isCheckedIn).orElse(false)) throw alreadyCheckedIn();
            throw new BookingException(CONFLICT, "TICKET_NOT_VALID", "This ticket does not belong to a paid booking.");
        }
        return new CheckInResponse(ticketId, now, staffId);
    }

    private static BookingException alreadyCheckedIn() {
        return new BookingException(CONFLICT, "TICKET_ALREADY_CHECKED_IN", "This ticket has already been checked in.");
    }
}
