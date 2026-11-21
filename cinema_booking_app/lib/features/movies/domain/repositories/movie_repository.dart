import '../../../../core/network/api_result.dart';
import '../entities/movie.dart';
import '../entities/movie_schedule.dart';

abstract interface class MovieRepository {
  /// [search] lọc theo tên phim ở phía server, trong phạm vi [schedule].
  Future<ApiResult<List<Movie>>> getMovies(
    MovieSchedule schedule, {
    String? search,
  });

  Future<ApiResult<Movie>> getMovieDetail(String movieId);
}
