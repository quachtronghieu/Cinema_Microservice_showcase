class DemoPaymentResult {
  const DemoPaymentResult({
    required this.bookingId,
    required this.paymentId,
    required this.paymentStatus,
    required this.amount,
    required this.message,
    this.ticketId,
  });

  final String bookingId;
  final String paymentId;
  final String paymentStatus;
  final int amount;
  final String? ticketId;
  final String message;

  /// booking-service returns SUCCEEDED (AGENTS 5.2); PAID is the legacy monolith value.
  bool get isSuccessful =>
      const {'SUCCEEDED', 'PAID'}.contains(paymentStatus.toUpperCase());
}

enum DemoPaymentOutcome {
  success,
  failed;

  String get apiValue => name.toUpperCase();
}
