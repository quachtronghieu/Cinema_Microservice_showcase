import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/entities/demo_payment.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/repositories/demo_payment_repository.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/presentation/view_models/demo_payment_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('stores the issued ticket when demo payment succeeds', () async {
    final repository = _FakeDemoPaymentRepository();
    final viewModel = DemoPaymentViewModel(repository);

    await viewModel.process(
      bookingId: 'BK000001',
      outcome: DemoPaymentOutcome.success,
    );

    expect(repository.outcome, DemoPaymentOutcome.success);
    expect(viewModel.result?.isSuccessful, isTrue);
    expect(viewModel.result?.ticketId, 'TK000001');
    expect(viewModel.error, isNull);
  });

  test('shows a failed demo payment without a ticket', () async {
    final repository = _FakeDemoPaymentRepository();
    final viewModel = DemoPaymentViewModel(repository);

    await viewModel.process(
      bookingId: 'BK000001',
      outcome: DemoPaymentOutcome.failed,
    );

    expect(repository.outcome, DemoPaymentOutcome.failed);
    expect(viewModel.result?.isSuccessful, isFalse);
    expect(viewModel.result?.ticketId, isNull);
    expect(viewModel.error, isNull);
  });

  test('treats the legacy PAID status as success too', () {
    const result = DemoPaymentResult(
      bookingId: 'BK000001',
      paymentId: 'PM000001',
      paymentStatus: 'PAID',
      amount: 120000,
      message: 'Paid.',
    );

    expect(result.isSuccessful, isTrue);
  });
}

class _FakeDemoPaymentRepository implements DemoPaymentRepository {
  DemoPaymentOutcome? outcome;

  @override
  Future<ApiResult<DemoPaymentResult>> process({
    required String bookingId,
    required DemoPaymentOutcome outcome,
  }) {
    this.outcome = outcome;
    final successful = outcome == DemoPaymentOutcome.success;
    return Future.value(
      ApiResult.success(
        DemoPaymentResult(
          bookingId: bookingId,
          paymentId: 'PM000001',
          paymentStatus: successful ? 'SUCCEEDED' : 'FAILED',
          amount: 120000,
          ticketId: successful ? 'TK000001' : null,
          message: successful ? 'Paid.' : 'Failed.',
        ),
      ),
    );
  }
}
