import '../../domain/entities/movie.dart';
import '../../domain/entities/movie_schedule.dart';

enum MovieLoadStatus { initial, loading, success, failure }

class MovieDiscoveryState {
  const MovieDiscoveryState({
    this.status = MovieLoadStatus.initial,
    this.schedule = MovieSchedule.nowShowing,
    this.movies = const [],
    this.query = '',
    this.category,
    this.errorMessage,
  });

  final MovieLoadStatus status;
  final MovieSchedule schedule;

  /// Toàn bộ phim của [schedule] do server trả về, chưa lọc.
  final List<Movie> movies;
  final String query;

  /// `null` nghĩa là "Tất cả".
  final String? category;
  final String? errorMessage;

  static const _unset = Object();

  MovieDiscoveryState copyWith({
    MovieLoadStatus? status,
    MovieSchedule? schedule,
    List<Movie>? movies,
    String? query,
    Object? category = _unset,
    Object? errorMessage = _unset,
  }) => MovieDiscoveryState(
    status: status ?? this.status,
    schedule: schedule ?? this.schedule,
    movies: movies ?? this.movies,
    query: query ?? this.query,
    category: identical(category, _unset) ? this.category : category as String?,
    errorMessage: identical(errorMessage, _unset)
        ? this.errorMessage
        : errorMessage as String?,
  );
}
