package vn.edu.fpt.cinemamanagement.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hold ghế đang hiệu lực của khách. Đây là dữ liệu bàn giao cho các bước sau
 * (bắp nước, báo giá, tạo đơn): {@code holdId}, {@code showtimeId},
 * {@code seatIds}, {@code expiresAt}, {@code serverTime}.
 *
 * <p>{@code expiresAt}/{@code serverTime} có offset múi giờ để client tính
 * thời gian còn lại theo đồng hồ server, không theo đồng hồ thiết bị.</p>
 */
public record SeatHoldResponseDTO(
        String holdId,
        String showtimeId,
        List<String> seatIds,
        OffsetDateTime expiresAt,
        OffsetDateTime serverTime
) {}
