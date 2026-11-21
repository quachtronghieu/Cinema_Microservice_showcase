package vn.edu.fpt.cinema.cinema.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeResponseDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeSeatMapResponse;
import vn.edu.fpt.cinema.cinema.service.ShowtimeService;

/**
 * Controller phục vụ các chức năng Suất Chiếu dành cho Khách hàng (Customer / Public)
 * Phụ trách bởi: Phan Đức Thọ
 * - View Showtimes (Xem danh sách suất chiếu chưa qua)
 * - Select Showtime (Chọn suất chiếu để xem thông tin, chặn nếu suất chiếu đã qua)
 */
@RestController
@RequestMapping("/api/v1/showtimes")
public class PublicShowtimeRestController {

    private final ShowtimeService showtimeService;

    public PublicShowtimeRestController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    /**
     * View Showtimes (Customer - Phan Đức Thọ)
     * Trả về danh sách các suất chiếu:
     * - Đang mở bán (OPEN)
     * - Chưa qua thời gian bắt đầu (startTime > LocalDateTime.now())
     * Hỗ trợ lọc theo movieId hoặc date
     */
    @GetMapping
    public ResponseEntity<List<ShowtimeResponseDTO>> getShowtimes(
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(showtimeService.getCustomerShowtimes(movieId, date));
    }

    /**
     * Select Showtime (Customer - Phan Đức Thọ)
     * Lấy thông tin chi tiết của 1 suất chiếu mà khách hàng chọn
     *
     * CHẶN NGHIỆP VỤ:
     * - Nếu suất chiếu đã diễn ra trong quá khứ -> Ném ShowtimeExpiredException (HTTP 400 Bad Request)
     * - Nếu suất chiếu chưa mở bán hoặc đã đóng -> Ném IllegalStateException (HTTP 409 Conflict)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponseDTO> selectShowtime(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.getCustomerSelectedShowtime(id));
    }

    /** Physical layout only: real availability per showtime is owned by booking-service (AGENTS 3). */
    @GetMapping("/{id}/seats")
    public ResponseEntity<ShowtimeSeatMapResponse> getSeatMap(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.getSeatMap(id));
    }
}
