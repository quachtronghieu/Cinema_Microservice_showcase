class BookingSelection {
  const BookingSelection({
    required this.holdId,
    required this.showtimeId,
    required this.showtimeSeatIds,
    this.concessions = const [],
    this.voucherCode,
  });

  final String holdId;
  final String showtimeId;
  final List<String> showtimeSeatIds;
  final List<ConcessionSelection> concessions;
  final String? voucherCode;

  BookingSelection copyWith({String? voucherCode}) => BookingSelection(
    holdId: holdId,
    showtimeId: showtimeId,
    showtimeSeatIds: showtimeSeatIds,
    concessions: concessions,
    voucherCode: voucherCode,
  );
}

class ConcessionSelection {
  const ConcessionSelection({
    required this.concessionId,
    required this.quantity,
  });

  final String concessionId;
  final int quantity;
}

class BookingLineItem {
  const BookingLineItem({
    required this.itemType,
    required this.itemId,
    required this.name,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  final String itemType;
  final String itemId;
  final String name;
  final int quantity;
  final int unitPrice;
  final int lineTotal;
}

class AppliedVoucher {
  const AppliedVoucher({
    required this.voucherId,
    required this.code,
    required this.discountType,
    required this.discountValue,
  });

  final String voucherId;
  final String code;
  final String discountType;
  final int discountValue;
}

class BookingQuote {
  const BookingQuote({
    required this.ticketItems,
    required this.concessionItems,
    required this.ticketSubtotal,
    required this.concessionSubtotal,
    required this.subtotal,
    required this.discountAmount,
    required this.total,
    this.voucher,
  });

  final List<BookingLineItem> ticketItems;
  final List<BookingLineItem> concessionItems;
  final int ticketSubtotal;
  final int concessionSubtotal;
  final int subtotal;
  final int discountAmount;
  final int total;
  final AppliedVoucher? voucher;
}

class CreatedBooking {
  const CreatedBooking({
    required this.bookingId,
    required this.status,
    required this.totalAmount,
  });

  final String bookingId;
  final String status;
  final int totalAmount;
}
