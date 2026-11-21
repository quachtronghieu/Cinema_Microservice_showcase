import '../../../../core/network/api_result.dart';
import '../../../movies/domain/entities/movie.dart';

abstract interface class WatchlistRepository {
  /// Phim khách đang lưu, mới thêm nhất trước; server là nguồn dữ liệu chính thức.
  Future<ApiResult<List<Movie>>> getWatchlistMovies();

  /// Idempotent: thêm phim đã có trong watchlist vẫn thành công.
  Future<ApiResult<void>> add(String movieId);

  /// Idempotent: bỏ phim chưa lưu vẫn thành công.
  Future<ApiResult<void>> remove(String movieId);
}
