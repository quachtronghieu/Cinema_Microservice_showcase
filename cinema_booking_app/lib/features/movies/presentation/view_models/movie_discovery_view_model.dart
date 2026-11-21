import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../../core/network/api_result.dart';
import '../../domain/entities/movie.dart';
import '../../domain/entities/movie_schedule.dart';
import '../../domain/repositories/movie_repository.dart';
import '../states/movie_discovery_state.dart';

class MovieDiscoveryViewModel extends ChangeNotifier {
  MovieDiscoveryViewModel(this._repository);

  /// Chờ người dùng gõ xong rồi mới gọi API tìm kiếm.
  static const searchDebounce = Duration(milliseconds: 300);

  final MovieRepository _repository;
  MovieDiscoveryState _state = const MovieDiscoveryState();
  int _requestId = 0;
  bool _disposed = false;
  Timer? _searchTimer;

  MovieDiscoveryState get state => _state;

  /// Thể loại xuất hiện trong danh sách hiện tại, theo thứ tự gặp đầu tiên.
  List<String> get categories {
    final seen = <String>{};
    final result = <String>[];
    for (final movie in _state.movies) {
      for (final genre in movie.genres) {
        if (seen.add(genre.toLowerCase())) result.add(genre);
      }
    }
    return result;
  }

  /// Lọc tại chỗ để lưới phản hồi ngay trong lúc chờ kết quả tìm kiếm từ server.
  List<Movie> get visibleMovies {
    final query = _state.query.trim().toLowerCase();
    final category = _state.category?.toLowerCase();
    return _state.movies
        .where((movie) {
          final matchesQuery =
              query.isEmpty || movie.title.toLowerCase().contains(query);
          final matchesCategory =
              category == null ||
              movie.genres.any((genre) => genre.toLowerCase() == category);
          return matchesQuery && matchesCategory;
        })
        .toList(growable: false);
  }

  Future<void> load() async {
    // Chỉ nhận kết quả của request mới nhất; request cũ (đổi tab nhanh) bị bỏ.
    _searchTimer?.cancel();
    final requestId = ++_requestId;
    final schedule = _state.schedule;
    final search = _state.query.trim();
    _emit(_state.copyWith(status: MovieLoadStatus.loading, errorMessage: null));

    final result = await _repository.getMovies(
      schedule,
      search: search.isEmpty ? null : search,
    );
    if (_disposed || requestId != _requestId) return;

    switch (result) {
      case Success(:final data):
        final category = _state.category;
        final categoryStillExists =
            category == null ||
            data.any(
              (movie) => movie.genres.any(
                (genre) => genre.toLowerCase() == category.toLowerCase(),
              ),
            );
        _emit(
          _state.copyWith(
            status: MovieLoadStatus.success,
            movies: data,
            category: categoryStillExists ? category : null,
          ),
        );
      case FailureResult(:final failure):
        _emit(
          _state.copyWith(
            status: MovieLoadStatus.failure,
            errorMessage: failure.message,
          ),
        );
    }
  }

  Future<void> selectSchedule(MovieSchedule schedule) async {
    if (_state.schedule == schedule) return;
    _emit(
      _state.copyWith(
        schedule: schedule,
        movies: const [],
        category: null,
        errorMessage: null,
      ),
    );
    await load();
  }

  void updateQuery(String query) {
    if (_state.query == query) return;
    final searchChanged = _state.query.trim() != query.trim();
    _emit(_state.copyWith(query: query));
    if (!searchChanged) return;
    _searchTimer?.cancel();
    _searchTimer = Timer(searchDebounce, load);
  }

  void selectCategory(String? category) {
    if (_state.category == category) return;
    _emit(_state.copyWith(category: category));
  }

  void _emit(MovieDiscoveryState next) {
    if (_disposed) return;
    _state = next;
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _searchTimer?.cancel();
    super.dispose();
  }
}
