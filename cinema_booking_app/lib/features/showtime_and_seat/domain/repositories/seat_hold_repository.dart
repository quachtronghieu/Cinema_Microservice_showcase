import 'package:cinema_booking_app/core/network/api_result.dart';

import '../entities/seat_hold.dart';

abstract interface class SeatHoldRepository {
  /// Giữ [seatIds] của suất chiếu; server thay thế hold cũ của khách cho
  /// cùng suất chiếu. Xung đột ghế trả `Failure` có
  /// `errors['code'] == 'SEAT_UNAVAILABLE'` và `errors['seatIds']`.
  Future<ApiResult<SeatHold>> holdSeats({
    required String showtimeId,
    required List<String> seatIds,
  });

  /// Nhả hold. Hold đã hết hạn/đã nhả được coi là thành công.
  Future<ApiResult<void>> releaseHold(String holdId);
}
