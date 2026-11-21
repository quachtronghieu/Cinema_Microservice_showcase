package vn.edu.fpt.cinemamanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CustomerTicketResponseDTO(
        String ticketId,
        String bookingId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        String roomId,
        List<String> seatLabels,
        List<ConcessionItem> concessionItems,
        BigDecimal totalAmount,
        boolean checkedIn
) {
    public record ConcessionItem(
            String concessionId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
