package vn.edu.fpt.cinema.cinema.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.cinema.dto.PageResponse;
import vn.edu.fpt.cinema.cinema.dto.RoomResponse;
import vn.edu.fpt.cinema.cinema.dto.RoomTemplateResponse;
import vn.edu.fpt.cinema.cinema.exception.ApiError;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;
import vn.edu.fpt.cinema.cinema.service.RoomService;

/** ADMIN room API. There is deliberately no create, delete, edit or template-change route (scope R, Task 1). */
@RestController
@RequestMapping("/api/v1/admin/rooms")
public class RoomRestController {

    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public PageResponse<RoomResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return roomService.list(page, size);
    }

    @GetMapping("/{id}")
    public RoomResponse detail(@PathVariable String id) {
        return roomService.detail(id);
    }

    @GetMapping("/{id}/template")
    public RoomTemplateResponse template(@PathVariable String id) {
        return roomService.template(id);
    }

    /** The body must be exactly {"status": "ACTIVE" | "INACTIVE"}; any other field is rejected. */
    @PatchMapping("/{id}/status")
    public RoomResponse changeStatus(@PathVariable String id, @RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        if (body == null || body.size() != 1 || !(body.get("status") instanceof String status)
                || !(status.equals(RoomService.ACTIVE) || status.equals(RoomService.INACTIVE))) {
            throw new CinemaException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Body chỉ gồm status với giá trị ACTIVE hoặc INACTIVE.");
        }
        String staffId = jwt.getClaimAsString("staffId");
        String actor = staffId == null || staffId.isBlank() ? jwt.getSubject() : staffId;
        return roomService.changeStatus(id, status, actor, ApiError.trace(request));
    }
}
