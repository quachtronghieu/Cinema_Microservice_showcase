package vn.edu.fpt.cinema.booking.service;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.cinema.booking.repository.SeatHoldRepository;

/** Gives seats back when a hold expires. It reads only the DB, so several instances can run it safely (AGENTS 5.1). */
@Component
public class SeatHoldExpiryJob {
    private static final Logger LOG = LoggerFactory.getLogger(SeatHoldExpiryJob.class);
    private static final int BATCH = 100;

    private final SeatHoldRepository holds;
    private final SeatHoldService service;
    private final Clock clock;

    public SeatHoldExpiryJob(SeatHoldRepository holds, SeatHoldService service, Clock clock) {
        this.holds = holds;
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${booking.seat-hold.expiry-delay:PT5S}", initialDelayString = "${booking.seat-hold.expiry-delay:PT5S}")
    public void expireOverdue() {
        holds.findExpiredIds(clock.instant(), PageRequest.of(0, BATCH)).forEach(holdId -> {
            try {
                service.expire(holdId);
            } catch (RuntimeException e) {
                // One failed hold must not stop the others; it is picked up again on the next run.
                LOG.warn("Seat hold expiry failed holdId={}", holdId, e);
            }
        });
    }
}
