import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../../domain/entities/movie_schedule.dart';

class MovieApiService {
  MovieApiService({Dio? dio}) : _dio = dio ?? ApiClient.instance.dio;

  /// Giới hạn `size` lớn nhất của API `/api/v1/movies`.
  static const pageSize = 100;

  final Dio _dio;

  Future<Response<dynamic>> getMovies(
    MovieSchedule schedule, {
    String? search,
    int page = 0,
  }) => _dio.get(
    ApiConfig.movies,
    queryParameters: {
      'status': _statusParam(schedule),
      'page': page,
      'size': pageSize,
      if (search != null && search.isNotEmpty) 'search': search,
    },
  );

  Future<Response<dynamic>> getMovieDetail(String movieId) =>
      _dio.get(ApiConfig.movieDetail(movieId));

  static String _statusParam(MovieSchedule schedule) => switch (schedule) {
    MovieSchedule.nowShowing => 'now-showing',
    MovieSchedule.comingSoon => 'coming-soon',
  };
}
