package vn.edu.fpt.cinemamanagement.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.cinemamanagement.services.CustomerSeatHoldService;

@Component
public class SeatHoldExpiryScheduler {
    private final CustomerSeatHoldService seatHoldService;

    public SeatHoldExpiryScheduler(CustomerSeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @Scheduled(fixedRate = 15000) // Scan per 15s
    public void releaseExpiredHolds() {
        seatHoldService.releaseExpiredHolds();
    }

    /** Hold chỉ nằm trong bộ nhớ nên sau khi khởi động lại phải trả các ghế PENDING mồ côi. */
    @EventListener(ApplicationReadyEvent.class)
    public void releaseOrphanPendingSeats() {
        seatHoldService.releaseOrphanPendingSeats();
    }
}
