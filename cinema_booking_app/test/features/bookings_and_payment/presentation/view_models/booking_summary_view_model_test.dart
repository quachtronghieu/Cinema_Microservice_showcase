import 'dart:async';

import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/entities/booking_summary.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/repositories/booking_summary_repository.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/presentation/view_models/booking_summary_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('loads the server-calculated booking quote', () async {
    final viewModel = BookingSummaryViewModel(_FakeBookingRepository());

    await viewModel.preview(
      const BookingSelection(
        holdId: 'hold-1',
        showtimeId: 'ST000001',
        showtimeSeatIds: ['SS000001'],
      ),
    );

    expect(viewModel.error, isNull);
    expect(viewModel.quote?.total, 120000);
  });

  test('forwards the idempotency key when creating a booking', () async {
    final repository = _FakeBookingRepository();
    final viewModel = BookingSummaryViewModel(repository);

    final created = await viewModel.create(
      const BookingSelection(
        holdId: 'hold-1',
        showtimeId: 'ST000001',
        showtimeSeatIds: ['SS000001'],
      ),
      idempotencyKey: 'booking-attempt-1',
    );

    expect(created?.bookingId, 'BK000001');
    expect(repository.lastIdempotencyKey, 'booking-attempt-1');
  });

  test('does not send a duplicate request while creation is pending', () async {
    final repository = _FakeBookingRepository(deferCreate: true);
    final viewModel = BookingSummaryViewModel(repository);
    const selection = BookingSelection(
      holdId: 'hold-1',
      showtimeId: 'ST000001',
      showtimeSeatIds: ['SS000001'],
    );

    final firstCreate = viewModel.create(
      selection,
      idempotencyKey: 'booking-attempt-2',
    );
    final secondResult = await viewModel.create(
      selection,
      idempotencyKey: 'booking-attempt-2',
    );

    expect(secondResult, isNull);
    expect(repository.createCalls, 1);

    repository.completeCreate();
    expect((await firstCreate)?.bookingId, 'BK000001');
  });
}

class _FakeBookingRepository implements BookingSummaryRepository {
  _FakeBookingRepository({this.deferCreate = false});

  final bool deferCreate;
  final Completer<ApiResult<CreatedBooking>> _createCompleter = Completer();
  String? lastIdempotencyKey;
  int createCalls = 0;

  @override
  Future<ApiResult<BookingQuote>> preview(BookingSelection selection) async =>
      ApiResult.success(
        const BookingQuote(
          ticketItems: [],
          concessionItems: [],
          ticketSubtotal: 120000,
          concessionSubtotal: 0,
          subtotal: 120000,
          discountAmount: 0,
          total: 120000,
        ),
      );

  @override
  Future<ApiResult<CreatedBooking>> create(
    BookingSelection selection, {
    required String idempotencyKey,
  }) {
    createCalls++;
    lastIdempotencyKey = idempotencyKey;
    if (deferCreate) return _createCompleter.future;
    return Future.value(_createdResult());
  }

  void completeCreate() => _createCompleter.complete(_createdResult());

  ApiResult<CreatedBooking> _createdResult() => ApiResult.success(
    const CreatedBooking(
      bookingId: 'BK000001',
      status: 'BOOKED',
      totalAmount: 120000,
    ),
  );
}
