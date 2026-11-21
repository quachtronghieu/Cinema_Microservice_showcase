package vn.edu.fpt.cinemamanagement.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.edu.fpt.cinemamanagement.services.CustomerBookingCancellationService;

/** Cancels unpaid customer bookings after the five-minute payment window. */
@Component
public class CustomerBookingExpiryScheduler {
    private static final int PAYMENT_WINDOW_MINUTES = 5;

    private final CustomerBookingCancellationService cancellationService;

    public CustomerBookingExpiryScheduler(CustomerBookingCancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @Scheduled(fixedDelay = 10000)
    public void cancelExpiredBookings() {
        cancellationService.cancelExpiredBookings(
                LocalDateTime.now().minusMinutes(PAYMENT_WINDOW_MINUTES));
    }
}
