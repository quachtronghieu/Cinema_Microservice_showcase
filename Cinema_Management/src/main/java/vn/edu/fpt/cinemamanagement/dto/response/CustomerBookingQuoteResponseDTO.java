package vn.edu.fpt.cinemamanagement.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CustomerBookingQuoteResponseDTO(
        List<Item> ticketItems,
        List<Item> concessionItems,
        BigDecimal ticketSubtotal,
        BigDecimal concessionSubtotal,
        BigDecimal subtotal,
        Voucher voucher,
        BigDecimal discountAmount,
        BigDecimal total
) {
    public record Item(
            String itemType,
            String itemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record Voucher(String voucherId, String code, String discountType, int discountValue) {}
}
