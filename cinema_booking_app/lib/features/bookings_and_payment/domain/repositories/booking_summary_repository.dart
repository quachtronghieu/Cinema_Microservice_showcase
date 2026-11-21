import '../../../../core/network/api_result.dart';
import '../entities/booking_summary.dart';

abstract interface class BookingSummaryRepository {
  Future<ApiResult<BookingQuote>> preview(BookingSelection selection);

  Future<ApiResult<CreatedBooking>> create(
    BookingSelection selection, {
    required String idempotencyKey,
  });
}
