import '../../../../core/network/api_result.dart';
import '../entities/demo_payment.dart';

abstract interface class DemoPaymentRepository {
  Future<ApiResult<DemoPaymentResult>> process({
    required String bookingId,
    required DemoPaymentOutcome outcome,
  });
}
