package vn.edu.fpt.cinema.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import vn.edu.fpt.cinema.booking.entity.ShowtimeSeat;
import vn.edu.fpt.cinema.booking.entity.Voucher;
import vn.edu.fpt.cinema.booking.dto.BookingQuoteResponse;
import vn.edu.fpt.cinema.booking.dto.BookingQuoteResponse.Item;
import vn.edu.fpt.cinema.booking.dto.BookingQuoteResponse.VoucherInfo;

/** Server-side price of a hold plus concessions and an optional voucher; reused by create booking. */
public record BookingQuote(
    List<ShowtimeSeat> seats,
    List<ConcessionLine> concessionLines,
    Voucher voucher,
    BigDecimal ticketSubtotal,
    BigDecimal concessionSubtotal,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal total
) {
    public record ConcessionLine(String concessionId, String name, BigDecimal unitPrice, int quantity) {
        public BigDecimal lineTotal() { return money(unitPrice.multiply(BigDecimal.valueOf(quantity))); }
    }

    static BookingQuote of(List<ShowtimeSeat> seats, List<ConcessionLine> lines, Voucher voucher) {
        BigDecimal tickets = money(seats.stream().map(ShowtimeSeat::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal concessions = money(lines.stream().map(ConcessionLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal subtotal = tickets.add(concessions);
        BigDecimal discount = voucher == null ? money(BigDecimal.ZERO) : voucher.discountFor(subtotal);
        return new BookingQuote(seats, lines, voucher, tickets, concessions, subtotal, discount, subtotal.subtract(discount));
    }

    public BookingQuoteResponse toResponse() {
        var tickets = seats.stream().map(seat -> new Item("SEAT", seat.getShowtimeSeatId(), seat.label(), 1,
            money(seat.getUnitPrice()), money(seat.getUnitPrice()))).toList();
        var items = concessionLines.stream().map(line -> new Item("CONCESSION", line.concessionId(), line.name(),
            line.quantity(), money(line.unitPrice()), line.lineTotal())).toList();
        var voucherInfo = voucher == null ? null : new VoucherInfo(voucher.getVoucherId(), voucher.getCode(),
            voucher.getDiscountType(), voucher.getDiscountValue());
        return new BookingQuoteResponse(tickets, items, ticketSubtotal, concessionSubtotal, subtotal, voucherInfo, discountAmount, total);
    }

    static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
