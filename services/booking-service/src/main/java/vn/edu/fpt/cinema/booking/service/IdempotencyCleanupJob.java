package vn.edu.fpt.cinema.booking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes idempotency records past their 24-hour retention. */
@Component
public class IdempotencyCleanupJob {
    private final IdempotencyService idempotency;

    public IdempotencyCleanupJob(IdempotencyService idempotency) { this.idempotency = idempotency; }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void purge() { idempotency.purgeExpired(); }
}
