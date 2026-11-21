package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import java.util.List;

/** Same shape as the legacy CustomerBookingQuoteResponseDTO so the Flutter summary screen keeps working. */
public record BookingQuoteResponse(
    List<Item> ticketItems,
    List<Item> concessionItems,
    BigDecimal ticketSubtotal,
    BigDecimal concessionSubtotal,
    BigDecimal subtotal,
    VoucherInfo voucher,
    BigDecimal discountAmount,
    BigDecimal total
) {
    public record Item(String itemType, String itemId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record VoucherInfo(String voucherId, String code, String discountType, BigDecimal discountValue) {}
}
