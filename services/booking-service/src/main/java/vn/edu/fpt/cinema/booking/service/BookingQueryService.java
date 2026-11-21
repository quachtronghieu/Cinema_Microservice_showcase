package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.dto.BookingDetailResponse;
import vn.edu.fpt.cinema.booking.dto.BookingDetailResponse.Concession;
import vn.edu.fpt.cinema.booking.dto.BookingDetailResponse.Seat;
import vn.edu.fpt.cinema.booking.dto.BookingSummaryItem;
import vn.edu.fpt.cinema.booking.dto.PageResponse;
import vn.edu.fpt.cinema.booking.dto.TicketResponse;
import vn.edu.fpt.cinema.booking.entity.*;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.*;

/** Customer reads of their own purchased bookings; never calls cinema or catalog. */
@Service
@Transactional(readOnly = true)
public class BookingQueryService {
    /** Only paid bookings are shown: these screens are the customer's purchased tickets (user decision). */
    static final String PAID = "PAID";
    /** show_date/start_time are the cinema's wall-clock time (legacy data, Vietnam); the JVM runs in UTC. */
    static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Row letters first, then the seat number as a number: A9 before A10. */
    static final Comparator<String> SEAT_ORDER = Comparator.comparing((String label) -> label.replaceAll("\\d+$", ""))
        .thenComparingInt(label -> label.matches(".*\\d+$") ? Integer.parseInt(label.replaceAll("^.*?(\\d+)$", "$1")) : 0);

    private final BookingRepository bookings;
    private final BookingDetailRepository details;
    private final SalesSnapshotRepository showtimes;
    private final VoucherRepository vouchers;
    private final PaymentRepository payments;
    private final TicketRepository tickets;
    private final Clock clock;

    public BookingQueryService(BookingRepository bookings, BookingDetailRepository details,
                               SalesSnapshotRepository showtimes, VoucherRepository vouchers,
                               PaymentRepository payments, TicketRepository tickets, Clock clock) {
        this.bookings = bookings;
        this.details = details;
        this.showtimes = showtimes;
        this.vouchers = vouchers;
        this.payments = payments;
        this.tickets = tickets;
        this.clock = clock;
    }

    public PageResponse<BookingSummaryItem> list(String customerId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BookingException(BAD_REQUEST, "VALIDATION_ERROR", "page must be >= 0 and size between 1 and 100.");
        }
        var result = bookings.findByCustomerIdAndStatus(customerId, PAID,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var ids = result.getContent().stream().map(Booking::getBookingId).toList();
        Map<String, List<String>> seatLabels = details.findByBookingIdInAndItemType(ids, "SEAT").stream()
            .collect(Collectors.groupingBy(BookingDetail::getBookingId,
                Collectors.mapping(BookingDetail::getItemName, Collectors.toList())));
        Map<String, SalesSnapshot> snapshots = showtimes.findAllById(
                result.getContent().stream().map(Booking::getShowtimeId).distinct().toList()).stream()
            .collect(Collectors.toMap(SalesSnapshot::getShowtimeId, Function.identity()));
        var items = result.getContent().stream().map(booking -> {
            SalesSnapshot showtime = snapshots.get(booking.getShowtimeId());
            return new BookingSummaryItem(booking.getBookingId(), booking.getStatus(), showtime.getMovieTitle(),
                showtime.getShowDate(), showtime.getStartTime(),
                seatLabels.getOrDefault(booking.getBookingId(), List.of()).stream().sorted(SEAT_ORDER).toList(),
                booking.getTotalAmount(), booking.getCreatedAt());
        }).toList();
        return new PageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** The caller's booking in any status; 404 when missing, 403 when it belongs to someone else. */
    public Booking ownBooking(String customerId, String bookingId) {
        Booking booking = bookings.findById(bookingId)
            .orElseThrow(() -> new BookingException(NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found."));
        if (!booking.getCustomerId().equals(customerId)) {
            throw new BookingException(FORBIDDEN, "BOOKING_NOT_OWNED", "This booking belongs to another customer.");
        }
        return booking;
    }

    public BookingDetailResponse detail(String customerId, String bookingId) {
        Booking booking = ownBooking(customerId, bookingId);
        if (!PAID.equals(booking.getStatus())) {
            throw new BookingException(NOT_FOUND, "BOOKING_NOT_PAID", "This booking has not been paid, so there is no ticket to show.");
        }
        SalesSnapshot showtime = showtimes.findById(booking.getShowtimeId()).orElseThrow();
        var lines = details.findByBookingIdOrderByItemName(bookingId);
        var seats = lines.stream().filter(line -> "SEAT".equals(line.getItemType()))
            .sorted(Comparator.comparing(BookingDetail::getItemName, SEAT_ORDER))
            .map(line -> new Seat(line.getItemName(), line.getSeatType(), line.getUnitPrice())).toList();
        var concessions = lines.stream().filter(line -> "CONCESSION".equals(line.getItemType()))
            .map(line -> new Concession(line.getItemName(), line.getQuantity(), line.getUnitPrice(), line.getTotalPrice())).toList();
        String voucherCode = booking.getVoucherId() == null ? null
            : vouchers.findById(booking.getVoucherId()).map(Voucher::getCode).orElse(null);
        var payment = payments.findByBookingId(bookingId)
            .map(p -> new BookingDetailResponse.Payment(p.getPaymentId(), p.getStatus(), p.getCompletedAt())).orElse(null);
        String ticketId = tickets.findByBookingId(bookingId).map(Ticket::getTicketId).orElse(null);
        return new BookingDetailResponse(booking.getBookingId(), booking.getStatus(), booking.getCreatedAt(),
            booking.getExpiresAt(), showtime.getMovieTitle(), showtime.getRoomName(), showtime.getShowDate(),
            showtime.getStartTime(), seats, concessions, voucherCode, booking.getDiscountAmount(),
            booking.getTotalAmount(), payment, ticketId);
    }

    /** Newest first, as the legacy endpoint and the Flutter list expect (plain array, no paging). */
    public List<TicketResponse> tickets(String customerId) {
        // shortcut: a few queries per ticket; batch like list() if a customer ever has hundreds of tickets.
        return tickets.findAllOfCustomer(customerId).stream().map(this::ticketResponse).toList();
    }

    public TicketResponse ticket(String customerId, String ticketId) {
        Ticket ticket = tickets.findById(ticketId)
            .orElseThrow(() -> new BookingException(NOT_FOUND, "TICKET_NOT_FOUND", "Ticket not found."));
        Booking booking = bookings.findById(ticket.getBookingId()).orElseThrow();
        if (!booking.getCustomerId().equals(customerId)) {
            throw new BookingException(FORBIDDEN, "TICKET_NOT_OWNED", "This ticket belongs to another customer.");
        }
        return ticketResponse(ticket, booking);
    }

    private TicketResponse ticketResponse(Ticket ticket) {
        return ticketResponse(ticket, bookings.findById(ticket.getBookingId()).orElseThrow());
    }

    private TicketResponse ticketResponse(Ticket ticket, Booking booking) {
        SalesSnapshot showtime = showtimes.findById(booking.getShowtimeId()).orElseThrow();
        var lines = details.findByBookingIdOrderByItemName(booking.getBookingId());
        var seatLabels = lines.stream().filter(line -> "SEAT".equals(line.getItemType()))
            .map(BookingDetail::getItemName).sorted(SEAT_ORDER).toList();
        var concessions = lines.stream().filter(line -> "CONCESSION".equals(line.getItemType()))
            .map(line -> new TicketResponse.ConcessionItem(line.getConcessionId(), line.getItemName(),
                line.getQuantity(), line.getUnitPrice(), line.getTotalPrice())).toList();
        return new TicketResponse(ticket.getTicketId(), booking.getBookingId(), showtime.getMovieTitle(),
            showtime.getShowDate(), showtime.getStartTime(), String.valueOf(showtime.getRoomId()), showtime.getRoomName(),
            seatLabels, concessions, booking.getTotalAmount(), ticket.isCheckedIn(),
            ticketStatus(ticket, showtime), ticket.getCheckedInAt(),
            // Only the server-issued ID: nothing personal goes into the QR code.
            "CINEMA-TICKET:" + ticket.getTicketId());
    }

    /** Same rule as the Flutter app: an unused ticket stops being valid when its showtime starts. */
    String ticketStatus(Ticket ticket, SalesSnapshot showtime) {
        if (ticket.isCheckedIn()) return "CHECKED_IN";
        var startsAt = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime()).atZone(CINEMA_ZONE).toInstant();
        return clock.instant().isBefore(startsAt) ? "VALID" : "EXPIRED";
    }
}
