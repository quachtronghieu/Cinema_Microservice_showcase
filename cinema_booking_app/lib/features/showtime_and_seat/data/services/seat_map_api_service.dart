import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';

class SeatMapApiService {
  SeatMapApiService(this._dio);

  final Dio _dio;

  /// Sơ đồ vật lý của phòng chiếu, do cinema-service sở hữu.
  Future<Response<dynamic>> getSeatLayout(String showtimeId) =>
      _dio.get(ApiConfig.showtimeSeats(showtimeId));

  /// Trạng thái ghế thực của suất chiếu, do booking-service sở hữu.
  Future<Response<dynamic>> getSeatAvailability(String showtimeId) =>
      _dio.get(ApiConfig.showtimeSeatAvailability(showtimeId));
}
