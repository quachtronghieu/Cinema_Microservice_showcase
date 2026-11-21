package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The legacy CustomerTicketResponseDTO fields the Flutter "Vé của tôi" screen reads (roomId, concessionItems,
 * totalAmount, checkedIn), plus status, checkedInAt, roomName and qrPayload.
 * status: VALID, CHECKED_IN, or EXPIRED once the showtime has started without a check-in.
 */
public record TicketResponse(
    String ticketId,
    String bookingId,
    String movieTitle,
    LocalDate showDate,
    LocalTime startTime,
    String roomId,
    String roomName,
    List<String> seatLabels,
    List<ConcessionItem> concessionItems,
    BigDecimal totalAmount,
    boolean checkedIn,
    String status,
    Instant checkedInAt,
    String qrPayload
) {
    public record ConcessionItem(String concessionId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
