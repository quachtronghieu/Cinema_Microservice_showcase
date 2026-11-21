import '../../domain/entities/customer_ticket.dart';

class CustomerTicketDto {
  const CustomerTicketDto(this._json);

  final Map<String, dynamic> _json;

  CustomerTicket toEntity() => CustomerTicket(
    ticketId: _string('ticketId'),
    bookingId: _string('bookingId'),
    movieTitle: _string('movieTitle'),
    showDate: DateTime.parse(_string('showDate')),
    startTime: _string('startTime'),
    roomId: _string('roomId'),
    seatLabels: _stringList('seatLabels'),
    concessionItems: _concessionItems(),
    totalAmount: _number('totalAmount').toInt(),
    checkedIn: _json['checkedIn'] == true,
  );

  String _string(String key) {
    final value = _json[key]?.toString().trim();
    if (value == null || value.isEmpty) {
      throw FormatException('Thông tin vé thiếu $key.');
    }
    return value;
  }

  num _number(String key) {
    final value = _json[key];
    if (value is num) return value;
    return num.parse(value?.toString() ?? '');
  }

  List<String> _stringList(String key) {
    final value = _json[key];
    if (value is! List) {
      throw FormatException('Thông tin vé thiếu $key.');
    }
    return value.map((item) => item.toString()).toList(growable: false);
  }

  List<CustomerTicketConcession> _concessionItems() {
    final value = _json['concessionItems'];
    if (value == null) return const [];
    if (value is! List) {
      throw const FormatException('Thong tin bap nuoc cua ve khong hop le.');
    }
    return value
        .map((item) {
          if (item is! Map) {
            throw const FormatException(
              'Thong tin bap nuoc cua ve khong hop le.',
            );
          }
          final json = Map<String, dynamic>.from(item);
          return CustomerTicketConcession(
            concessionId: _requiredString(json, 'concessionId'),
            name: _requiredString(json, 'name'),
            quantity: _requiredNumber(json, 'quantity').toInt(),
            unitPrice: _requiredNumber(json, 'unitPrice').toInt(),
            lineTotal: _requiredNumber(json, 'lineTotal').toInt(),
          );
        })
        .toList(growable: false);
  }

  String _requiredString(Map<String, dynamic> json, String key) {
    final value = json[key]?.toString().trim();
    if (value == null || value.isEmpty) {
      throw FormatException('Thong tin ve thieu $key.');
    }
    return value;
  }

  num _requiredNumber(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is num) return value;
    return num.parse(value?.toString() ?? '');
  }
}
