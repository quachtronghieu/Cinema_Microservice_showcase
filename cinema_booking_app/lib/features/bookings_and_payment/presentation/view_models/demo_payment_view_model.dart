import 'package:flutter/foundation.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_result.dart';
import '../../data/repositories/demo_payment_repository_impl.dart';
import '../../data/services/demo_payment_api_service.dart';
import '../../domain/entities/demo_payment.dart';
import '../../domain/repositories/demo_payment_repository.dart';

class DemoPaymentViewModel extends ChangeNotifier {
  DemoPaymentViewModel([DemoPaymentRepository? repository])
    : _repository =
          repository ??
          DemoPaymentRepositoryImpl(
            DemoPaymentApiService(dio: ApiClient.instance.dio),
          );

  final DemoPaymentRepository _repository;
  DemoPaymentResult? _result;
  String? _error;
  bool _submitting = false;

  DemoPaymentResult? get result => _result;
  String? get error => _error;
  bool get submitting => _submitting;

  Future<void> process({
    required String bookingId,
    required DemoPaymentOutcome outcome,
  }) async {
    if (_submitting) return;
    _submitting = true;
    _error = null;
    _result = null;
    notifyListeners();

    final response = await _repository.process(
      bookingId: bookingId,
      outcome: outcome,
    );
    _submitting = false;
    if (response case Success<DemoPaymentResult>(:final data)) {
      _result = data;
    } else {
      _error = (response as FailureResult<DemoPaymentResult>).failure.message;
    }
    notifyListeners();
  }

  void retry() {
    _result = null;
    _error = null;
    notifyListeners();
  }
}
