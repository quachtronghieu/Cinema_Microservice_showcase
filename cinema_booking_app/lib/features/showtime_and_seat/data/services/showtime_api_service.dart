import 'package:dio/dio.dart';
import 'package:intl/intl.dart';

import '../../../../core/config/api_config.dart';

class ShowtimeApiService {
  ShowtimeApiService({required this._dio});

  final Dio _dio;

  Future<Response<dynamic>> getByMovieAndDate({
    required String movieId,
    required DateTime date,
  }) {
    return _dio.get(
      ApiConfig.showtimes,
      queryParameters: {
        'movieId': movieId,
        'date': DateFormat('yyyy-MM-dd').format(date),
      },
    );
  }
}
