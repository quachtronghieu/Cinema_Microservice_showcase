package vn.edu.fpt.cinema.booking.service;

import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.cinema.booking.repository.BookingRepository;

/** Expires bookings whose 5-minute payment window has passed; reads the shared DB, not memory (AGENTS 5.1). */
@Component
public class BookingExpiryJob {
    private static final int BATCH = 100;

    private final BookingRepository bookings;
    private final BookingReleaseService release;
    private final Clock clock;

    public BookingExpiryJob(BookingRepository bookings, BookingReleaseService release, Clock clock) {
        this.bookings = bookings;
        this.release = release;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT30S")
    public void expireOverdue() {
        // Each booking closes in its own transaction; a concurrent payment or cancel simply wins the gate.
        bookings.findOverdueIds(clock.instant(), PageRequest.of(0, BATCH)).forEach(id -> release.release(id, "EXPIRED"));
    }
}
