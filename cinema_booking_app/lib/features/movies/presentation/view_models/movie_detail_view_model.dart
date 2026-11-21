import 'package:flutter/foundation.dart';

import '../../../../core/network/api_result.dart';
import '../../domain/repositories/movie_repository.dart';
import '../states/movie_detail_state.dart';
import '../states/movie_discovery_state.dart';

class MovieDetailViewModel extends ChangeNotifier {
  MovieDetailViewModel(this._repository, {required this.movieId});

  final MovieRepository _repository;
  final String movieId;
  MovieDetailState _state = const MovieDetailState();
  bool _disposed = false;

  MovieDetailState get state => _state;

  Future<void> load() async {
    _emit(
      MovieDetailState(status: MovieLoadStatus.loading, movie: _state.movie),
    );

    final result = await _repository.getMovieDetail(movieId);
    if (_disposed) return;

    switch (result) {
      case Success(:final data):
        _emit(MovieDetailState(status: MovieLoadStatus.success, movie: data));
      case FailureResult(:final failure):
        _emit(
          MovieDetailState(
            status: MovieLoadStatus.failure,
            errorMessage: failure.message,
            notFound: failure.statusCode == 404,
          ),
        );
    }
  }

  void _emit(MovieDetailState next) {
    if (_disposed) return;
    _state = next;
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
