package vn.edu.fpt.cinemamanagement.dto.response;

import java.math.BigDecimal;

public record DemoPaymentResponseDTO(
        String bookingId,
        String paymentId,
        String paymentStatus,
        BigDecimal amount,
        String ticketId,
        String message
) {}
