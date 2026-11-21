import 'package:cinema_booking_app/core/network/api_result.dart';

import '../entities/seat_map.dart';

abstract interface class SeatMapRepository {
  Future<ApiResult<SeatMap>> getSeatMap(String showtimeId);
}
