import 'dart:async';

import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/watchlist/domain/repositories/watchlist_repository.dart';
import 'package:cinema_booking_app/features/watchlist/presentation/view_models/watchlist_view_model.dart';

import '../movies/movie_test_fakes.dart';

/// Repository giả giữ watchlist trong bộ nhớ như server thật: thêm/bỏ phim làm
/// thay đổi kết quả của lần tải kế tiếp.
class FakeWatchlistRepository implements WatchlistRepository {
  /// Phim đang lưu, mới thêm nhất trước.
  final List<Movie> saved = [];

  /// Đặt để mô phỏng lỗi tải danh sách.
  ApiResult<List<Movie>>? listFailure;
  ApiResult<void> writeResult = ApiResult.success(null);

  /// Khi đặt, thao tác ghi chờ completer này để kiểm tra trạng thái đang xử lý.
  Completer<ApiResult<void>>? pendingWrite;
  final List<String> added = [];
  final List<String> removed = [];

  @override
  Future<ApiResult<List<Movie>>> getWatchlistMovies() async =>
      listFailure ?? ApiResult.success(List.of(saved));

  @override
  Future<ApiResult<void>> add(String movieId) async {
    added.add(movieId);
    final result = await (pendingWrite?.future ?? Future.value(writeResult));
    if (result is Success<void> && !saved.any((movie) => movie.id == movieId)) {
      saved.insert(0, buildMovie(id: movieId, title: 'Phim $movieId'));
    }
    return result;
  }

  @override
  Future<ApiResult<void>> remove(String movieId) async {
    removed.add(movieId);
    final result = await (pendingWrite?.future ?? Future.value(writeResult));
    if (result is Success<void>) {
      saved.removeWhere((movie) => movie.id == movieId);
    }
    return result;
  }
}

/// ViewModel đã đăng nhập; gọi `pumpAndSettle` để phiên được đồng bộ.
WatchlistViewModel buildWatchlistViewModel(
  FakeWatchlistRepository repository, {
  bool signedIn = true,
}) =>
    WatchlistViewModel(repository)..syncSession(signedIn ? 'KH001:user' : null);

const watchlistFailure = Failure('Không thể thêm phim vào watchlist.');
