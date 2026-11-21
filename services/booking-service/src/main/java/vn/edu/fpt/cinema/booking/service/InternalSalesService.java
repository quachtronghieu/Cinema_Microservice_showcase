package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.dto.InitSalesRequest;
import vn.edu.fpt.cinema.booking.entity.SalesSnapshot;
import vn.edu.fpt.cinema.booking.entity.ShowtimeSeat;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.SalesSnapshotRepository;
import vn.edu.fpt.cinema.booking.repository.ShowtimeSeatRepository;

/** Open/close sales commands from cinema-service (AGENTS 4 and 23). */
@Service
public class InternalSalesService {
    private static final Logger log = LoggerFactory.getLogger(InternalSalesService.class);

    private final SalesSnapshotRepository salesSnapshots;
    private final ShowtimeSeatRepository showtimeSeats;

    public InternalSalesService(SalesSnapshotRepository salesSnapshots, ShowtimeSeatRepository showtimeSeats) {
        this.salesSnapshots = salesSnapshots;
        this.showtimeSeats = showtimeSeats;
    }

    /**
     * Idempotent: the first call stores the snapshot and its seats together; a retry for an OPEN showtime changes
     * nothing. A CLOSED showtime is never reopened.
     * Concurrent first calls for one showtime are not expected: cinema serializes open-sales with the room lock.
     */
    @Transactional
    public void initSales(InitSalesRequest request) {
        var existing = salesSnapshots.findById(request.showtimeId());
        if (existing.isPresent()) {
            if (!existing.get().isOpen()) {
                throw new BookingException(CONFLICT, "SALES_CLOSED", "Sales for this showtime were closed and cannot be reopened.");
            }
            return;
        }
        salesSnapshots.saveAndFlush(new SalesSnapshot(request.showtimeId(), 1L, "OPEN", request.movieId(),
            request.movieTitle(), request.roomId(), request.roomName(), request.showDate(), request.startTime(),
            request.endTime()));
        showtimeSeats.saveAll(request.seats().stream().map(seat -> new ShowtimeSeat(UUID.randomUUID().toString(),
            request.showtimeId(), seat.templateSeatId(), seat.rowLabel(), seat.seatNumber(),
            seat.seatType().toUpperCase(Locale.ROOT), seat.unitPrice(), "AVAILABLE", null)).toList());
        log.info("Opened sales for showtime {} with {} seats", request.showtimeId(), request.seats().size());
    }

    /** Idempotent: closing an unknown or already closed showtime changes nothing. */
    @Transactional
    public void closeSales(String showtimeId) {
        if (salesSnapshots.close(showtimeId) == 1) log.info("Closed sales for showtime {}", showtimeId);
    }
}
