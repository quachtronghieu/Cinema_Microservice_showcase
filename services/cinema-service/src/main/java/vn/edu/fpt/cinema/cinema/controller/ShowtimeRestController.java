package vn.edu.fpt.cinema.cinema.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeRequestDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeResponseDTO;
import vn.edu.fpt.cinema.cinema.service.ShowtimeService;

/**
 * Controller phục vụ các chức năng Quản lý Suất Chiếu dành cho ADMIN
 * Phụ trách bởi: Phan Đức Thọ
 * - View Showtimes For Admin
 * - Create / Update / Delete Showtime
 * - Open / Close Showtime Sales (sync seat inventory)
 */
@RestController
@RequestMapping("/api/v1/admin/showtimes")
public class ShowtimeRestController {

    private final ShowtimeService showtimeService;

    public ShowtimeRestController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    /**
     * View Showtimes For Admin
     */
    @GetMapping
    public ResponseEntity<List<ShowtimeResponseDTO>> list() {
        return ResponseEntity.ok(showtimeService.getAllShowtimes());
    }

    /**
     * Get Showtime Detail For Admin
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        ShowtimeResponseDTO showtime = showtimeService.getShowtimeById(id);
        if (showtime == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy suất chiếu ID: " + id));
        }
        return ResponseEntity.ok(showtime);
    }

    /**
     * Create Showtime (Admin)
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ShowtimeRequestDTO dto) {
        try {
            ShowtimeResponseDTO created = showtimeService.createShowtime(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Update Showtime (Admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ShowtimeRequestDTO dto) {
        try {
            ShowtimeResponseDTO updated = showtimeService.updateShowtime(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete Showtime (Admin)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!showtimeService.deleteShowtime(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy suất chiếu ID: " + id));
        }
        return ResponseEntity.ok(Map.of("message", "Xóa suất chiếu thành công", "showtimeId", id));
    }

    /**
     * Open Showtime Sales (sync seat inventory) (Admin)
     * Chuyển trạng thái sang OPEN và tự động đồng bộ seat inventory từ Template
     */
    @PutMapping("/{id}/open-sales")
    public ResponseEntity<?> openSales(@PathVariable String id) {
        try {
            ShowtimeResponseDTO result = showtimeService.openShowtimeSales(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Mở bán suất chiếu và đồng bộ danh sách ghế thành công",
                    "showtime", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Close Showtime Sales (Admin)
     * Chuyển trạng thái sang CLOSED để ngừng nhận đặt vé
     */
    @PutMapping("/{id}/close-sales")
    public ResponseEntity<?> closeSales(@PathVariable String id) {
        try {
            ShowtimeResponseDTO result = showtimeService.closeShowtimeSales(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Đóng bán suất chiếu thành công",
                    "showtime", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
