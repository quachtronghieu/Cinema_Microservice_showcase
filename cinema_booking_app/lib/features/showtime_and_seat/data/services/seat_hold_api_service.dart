import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';

class SeatHoldApiService {
  SeatHoldApiService(this._dio);

  final Dio _dio;

  Future<Response<dynamic>> holdSeats({
    required String showtimeId,
    required List<String> seatIds,
  }) => _dio.post(
    ApiConfig.customerSeatHolds,
    data: {'showtimeId': showtimeId, 'showtimeSeatIds': seatIds},
  );

  Future<Response<dynamic>> releaseHold(String holdId) =>
      _dio.delete(ApiConfig.customerSeatHold(holdId));
}
