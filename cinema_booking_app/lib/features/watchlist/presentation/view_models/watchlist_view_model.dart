import 'package:flutter/foundation.dart';

import '../../../../core/network/api_result.dart';
import '../../../movies/domain/entities/movie.dart';
import '../../domain/repositories/watchlist_repository.dart';

enum WatchlistToggleOutcome {
  added,
  removed,

  /// Chưa đăng nhập nên không có watchlist để ghi.
  signInRequired,

  /// Phim này đang có một thao tác chưa xong.
  ignored,
  failed,
}

enum WatchlistLoadStatus { initial, loading, success, failure }

/// Trạng thái watchlist của khách đang đăng nhập, dùng chung cho mọi màn hình
/// phim. Server là nguồn dữ liệu chính thức; thay đổi được áp dụng lạc quan
/// rồi hoàn tác nếu server từ chối.
class WatchlistViewModel extends ChangeNotifier {
  WatchlistViewModel(this._repository);

  final WatchlistRepository _repository;
  List<Movie> _movies = const [];
  Set<String> _movieIds = const {};
  final Set<String> _pendingIds = {};
  WatchlistLoadStatus _status = WatchlistLoadStatus.initial;
  String? _loadError;
  String? _sessionKey;
  bool _sessionInitialized = false;
  // Tăng mỗi khi đổi phiên để bỏ kết quả của request thuộc phiên cũ.
  int _generation = 0;
  bool _disposed = false;

  /// Thông báo của lần [toggle] gần nhất bị [WatchlistToggleOutcome.failed].
  String? errorMessage;

  bool get isSignedIn => _sessionKey != null;
  WatchlistLoadStatus get status => _status;
  String? get loadError => _loadError;

  /// Phim đã lưu để hiển thị. Bỏ phim thì biến mất ngay; phim vừa thêm xuất
  /// hiện sau khi danh sách được tải lại từ server.
  List<Movie> get savedMovies => _movies
      .where((movie) => _movieIds.contains(movie.id))
      .toList(growable: false);

  bool contains(String movieId) => _movieIds.contains(movieId);

  bool isPending(String movieId) => _pendingIds.contains(movieId);

  void syncSession(String? sessionKey) {
    if (_disposed || (_sessionInitialized && _sessionKey == sessionKey)) {
      return;
    }
    _sessionInitialized = true;
    _sessionKey = sessionKey;
    _generation++;
    final generation = _generation;

    Future<void>.microtask(() async {
      if (_disposed || generation != _generation) return;
      _movies = const [];
      _movieIds = const {};
      _pendingIds.clear();
      _status = WatchlistLoadStatus.initial;
      _loadError = null;
      notifyListeners();
      if (sessionKey != null) await load();
    });
  }

  Future<void> load() async {
    if (_sessionKey == null) return;
    final generation = _generation;
    if (_movies.isEmpty) {
      _status = WatchlistLoadStatus.loading;
      notifyListeners();
    }

    final result = await _repository.getWatchlistMovies();
    if (_disposed || generation != _generation) return;

    // Lỗi tải không xóa dữ liệu đang có: thao tác thêm/bỏ vẫn dùng được.
    switch (result) {
      case Success(:final data):
        _movies = data;
        _movieIds = {for (final movie in data) movie.id};
        _status = WatchlistLoadStatus.success;
        _loadError = null;
      case FailureResult(:final failure):
        _status = WatchlistLoadStatus.failure;
        _loadError = failure.message;
    }
    notifyListeners();
  }

  Future<WatchlistToggleOutcome> toggle(String movieId) async {
    if (_sessionKey == null) return WatchlistToggleOutcome.signInRequired;
    if (_pendingIds.contains(movieId)) return WatchlistToggleOutcome.ignored;

    final generation = _generation;
    final wasSaved = _movieIds.contains(movieId);
    _setSaved(movieId, !wasSaved);
    _pendingIds.add(movieId);
    notifyListeners();

    final result = wasSaved
        ? await _repository.remove(movieId)
        : await _repository.add(movieId);
    if (_disposed || generation != _generation) {
      return WatchlistToggleOutcome.ignored;
    }

    _pendingIds.remove(movieId);
    switch (result) {
      case Success():
        notifyListeners();
        // Phim vừa thêm chưa có thông tin trong danh sách: tải lại từ server.
        if (!wasSaved) await load();
        return wasSaved
            ? WatchlistToggleOutcome.removed
            : WatchlistToggleOutcome.added;
      case FailureResult(:final failure):
        _setSaved(movieId, wasSaved);
        errorMessage = failure.message;
        notifyListeners();
        return WatchlistToggleOutcome.failed;
    }
  }

  void _setSaved(String movieId, bool saved) {
    _movieIds = saved
        ? {..._movieIds, movieId}
        : (_movieIds.toSet()..remove(movieId));
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
