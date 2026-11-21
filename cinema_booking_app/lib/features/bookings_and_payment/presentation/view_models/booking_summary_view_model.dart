import 'package:flutter/foundation.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_result.dart';
import '../../data/repositories/booking_summary_repository_impl.dart';
import '../../data/services/booking_summary_api_service.dart';
import '../../domain/entities/booking_summary.dart';
import '../../domain/repositories/booking_summary_repository.dart';

class BookingSummaryViewModel extends ChangeNotifier {
  BookingSummaryViewModel([BookingSummaryRepository? repository])
    : _repository =
          repository ??
          BookingSummaryRepositoryImpl(
            BookingSummaryApiService(dio: ApiClient.instance.dio),
          );

  final BookingSummaryRepository _repository;
  BookingQuote? _quote;
  String? _error;
  bool _loading = false;
  bool _submitting = false;
  bool _disposed = false;
  int _previewRequest = 0;

  BookingQuote? get quote => _quote;
  String? get error => _error;
  bool get loading => _loading;
  bool get submitting => _submitting;

  Future<void> preview(BookingSelection selection) async {
    if (_disposed || _submitting) {
      return;
    }

    final request = ++_previewRequest;

    _loading = true;
    _error = null;
    _quote = null;
    notifyListeners();

    final result = await _repository.preview(selection);

    if (_disposed || request != _previewRequest) {
      return;
    }

    if (result case Success<BookingQuote>(:final data)) {
      _quote = data;
    } else {
      _error = (result as FailureResult<BookingQuote>).failure.message;
    }

    _loading = false;
    notifyListeners();
  }

  Future<CreatedBooking?> create(
    BookingSelection selection, {
    required String idempotencyKey,
  }) async {
    if (_disposed || _submitting || _loading) {
      return null;
    }
    _submitting = true;
    _error = null;
    notifyListeners();
    final result = await _repository.create(
      selection,
      idempotencyKey: idempotencyKey,
    );
    if (_disposed) {
      return null;
    }
    _submitting = false;
    if (result case Success<CreatedBooking>(:final data)) {
      notifyListeners();
      return data;
    }
    _error = (result as FailureResult<CreatedBooking>).failure.message;
    notifyListeners();
    return null;
  }

  @override
  void dispose() {
    _disposed = true;
    _previewRequest++;
    super.dispose();
  }
}
