package vn.edu.fpt.cinema.booking.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record VoucherPage(List<VoucherResponse> items, int page, int size, long totalElements, int totalPages) {
    public VoucherPage {
        items = List.copyOf(items);
    }

    public static VoucherPage from(Page<VoucherResponse> page) {
        return new VoucherPage(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }
}
