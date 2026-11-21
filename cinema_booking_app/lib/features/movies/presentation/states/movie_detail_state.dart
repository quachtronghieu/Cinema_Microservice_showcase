import '../../domain/entities/movie.dart';
import 'movie_discovery_state.dart';

class MovieDetailState {
  const MovieDetailState({
    this.status = MovieLoadStatus.initial,
    this.movie,
    this.errorMessage,
    this.notFound = false,
  });

  final MovieLoadStatus status;
  final Movie? movie;
  final String? errorMessage;

  /// Phim không tồn tại: thử lại không có ích nên UI ẩn nút Retry.
  final bool notFound;
}
