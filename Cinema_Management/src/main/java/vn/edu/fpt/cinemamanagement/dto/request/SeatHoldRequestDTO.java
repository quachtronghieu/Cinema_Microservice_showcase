package vn.edu.fpt.cinemamanagement.dto.request;

import java.util.List;

/** Body của {@code POST /api/customer/seat-holds}. */
public record SeatHoldRequestDTO(
        String showtimeId,
        List<String> showtimeSeatIds
) {}
