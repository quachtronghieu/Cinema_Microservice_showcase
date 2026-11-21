package vn.edu.fpt.cinemamanagement.dto.response;

import java.math.BigDecimal;

public record CustomerBookingCreatedResponseDTO(
        String bookingId,
        String status,
        BigDecimal totalAmount
) {}
