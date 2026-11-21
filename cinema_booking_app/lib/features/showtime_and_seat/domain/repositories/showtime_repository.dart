import 'package:cinema_booking_app/core/network/api_result.dart';

import '../entities/showtime.dart';

abstract interface class ShowtimeRepository {
  Future<ApiResult<List<Showtime>>> getByMovieAndDate({
    required String movieId,
    required DateTime date,
  });
}
