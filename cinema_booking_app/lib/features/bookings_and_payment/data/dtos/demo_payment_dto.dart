import '../../domain/entities/demo_payment.dart';

class DemoPaymentRequestDto {
  const DemoPaymentRequestDto(this.outcome);

  final DemoPaymentOutcome outcome;

  Map<String, dynamic> toJson() => {'outcome': outcome.apiValue};
}

class DemoPaymentResultDto {
  const DemoPaymentResultDto(this._json);

  final Map<String, dynamic> _json;

  DemoPaymentResult toEntity() => DemoPaymentResult(
    bookingId: _requiredString('bookingId'),
    paymentId: _requiredString('paymentId'),
    paymentStatus: _requiredString('paymentStatus'),
    amount: _requiredNumber('amount').toInt(),
    ticketId: _json['ticketId']?.toString(),
    message: _requiredString('message'),
  );

  String _requiredString(String key) {
    final value = _json[key]?.toString().trim();
    if (value == null || value.isEmpty) {
      throw FormatException('Phản hồi thanh toán thiếu $key.');
    }
    return value;
  }

  num _requiredNumber(String key) {
    final value = _json[key];
    if (value is num) return value;
    return num.parse(value?.toString() ?? '');
  }
}
