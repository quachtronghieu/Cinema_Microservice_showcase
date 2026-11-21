import '../../domain/entities/booking_summary.dart';

class CustomerBookingRequestDto {
  const CustomerBookingRequestDto(this.selection);

  final BookingSelection selection;

  Map<String, dynamic> toJson() => {
    'holdId': selection.holdId.trim(),
    'showtimeId': selection.showtimeId,
    'showtimeSeatIds': selection.showtimeSeatIds,
    'concessions': selection.concessions
        .map(
          (item) => {
            'concessionId': item.concessionId,
            'quantity': item.quantity,
          },
        )
        .toList(),
    if (selection.voucherCode?.trim().isNotEmpty == true)
      'voucherCode': selection.voucherCode!.trim(),
  };
}

class BookingQuoteDto {
  const BookingQuoteDto(this._json);

  final Map<String, dynamic> _json;

  BookingQuote toEntity() => BookingQuote(
    ticketItems: _items('ticketItems'),
    concessionItems: _items('concessionItems'),
    ticketSubtotal: _money(_json['ticketSubtotal']),
    concessionSubtotal: _money(_json['concessionSubtotal']),
    subtotal: _money(_json['subtotal']),
    discountAmount: _money(_json['discountAmount']),
    total: _money(_json['total']),
    voucher: _json['voucher'] is Map<String, dynamic>
        ? _voucher(Map<String, dynamic>.from(_json['voucher'] as Map))
        : null,
  );

  List<BookingLineItem> _items(String key) =>
      ((_json[key] as List?) ?? const []).whereType<Map>().map((item) {
        final value = Map<String, dynamic>.from(item);
        return BookingLineItem(
          itemType: value['itemType']?.toString() ?? '',
          itemId: value['itemId']?.toString() ?? '',
          name: value['name']?.toString() ?? '',
          quantity: (value['quantity'] as num?)?.toInt() ?? 0,
          unitPrice: _money(value['unitPrice']),
          lineTotal: _money(value['lineTotal']),
        );
      }).toList();

  AppliedVoucher _voucher(Map<String, dynamic> value) => AppliedVoucher(
    voucherId: value['voucherId']?.toString() ?? '',
    code: value['code']?.toString() ?? '',
    discountType: value['discountType']?.toString() ?? '',
    discountValue: (value['discountValue'] as num?)?.toInt() ?? 0,
  );
}

class CreatedBookingDto {
  const CreatedBookingDto(this._json);

  final Map<String, dynamic> _json;

  CreatedBooking toEntity() => CreatedBooking(
    bookingId: _json['bookingId']?.toString() ?? '',
    status: _json['status']?.toString() ?? '',
    totalAmount: _money(_json['totalAmount']),
  );
}

int _money(dynamic value) => value is num ? value.round() : 0;
