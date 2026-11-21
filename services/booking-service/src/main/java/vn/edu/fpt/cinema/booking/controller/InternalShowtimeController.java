package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.InitSalesRequest;
import vn.edu.fpt.cinema.booking.service.InternalSalesService;

/** Called only by cinema-service with a service token holding scope sales:write (see SecurityConfiguration). */
@RestController
@RequestMapping("/api/v1/internal/showtimes")
public class InternalShowtimeController {
    private final InternalSalesService salesService;

    public InternalShowtimeController(InternalSalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping("/init-sales")
    public Map<String, String> initSales(@Valid @RequestBody InitSalesRequest request) {
        salesService.initSales(request);
        return Map.of("showtimeId", request.showtimeId(), "salesStatus", "OPEN");
    }

    @PostMapping("/{showtimeId}/close-sales")
    public Map<String, String> closeSales(@PathVariable String showtimeId) {
        salesService.closeSales(showtimeId);
        return Map.of("showtimeId", showtimeId, "salesStatus", "CLOSED");
    }
}
