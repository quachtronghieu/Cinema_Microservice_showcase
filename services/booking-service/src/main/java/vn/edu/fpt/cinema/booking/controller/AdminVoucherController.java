package vn.edu.fpt.cinema.booking.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.booking.dto.CreateVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.EditVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.VoucherPage;
import vn.edu.fpt.cinema.booking.dto.VoucherResponse;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.service.AdminVoucherService;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {
    private final AdminVoucherService vouchers;

    public AdminVoucherController(AdminVoucherService vouchers) { this.vouchers = vouchers; }

    @GetMapping
    public VoucherPage list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String status) {
        if (status != null && !status.equals("true") && !status.equals("false")) throw invalidInput();
        return vouchers.list(status == null ? null : Boolean.valueOf(status), page, size);
    }

    @GetMapping("/{id}")
    public VoucherResponse detail(@PathVariable String id) { return vouchers.detail(id); }

    @PostMapping
    public ResponseEntity<VoucherResponse> create(@Valid @RequestBody CreateVoucherRequest request) {
        var voucher = vouchers.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/vouchers/" + voucher.voucherId())).body(voucher);
    }

    @PatchMapping("/{id}")
    public VoucherResponse edit(@PathVariable String id, @Valid @RequestBody EditVoucherRequest request) {
        return vouchers.edit(id, request);
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable String id,
                                       @RequestBody(required = false) byte[] body,
                                       @RequestParam Map<String, String> parameters) {
        if ((body != null && body.length > 0) || !parameters.isEmpty()) throw invalidInput();
        vouchers.disable(id);
        return ResponseEntity.noContent().build();
    }

    private static BookingException invalidInput() {
        return new BookingException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid voucher request input");
    }
}
