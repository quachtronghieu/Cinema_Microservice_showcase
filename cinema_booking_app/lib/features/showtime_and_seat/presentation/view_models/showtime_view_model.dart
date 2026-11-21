import 'package:flutter/material.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/movies/domain/repositories/movie_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/showtime.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/presentation/states/showtime_state.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/showtime_repository.dart';

class ShowtimeViewModel extends ChangeNotifier {
  ShowtimeViewModel(this._repository, this._movieRepository);

  final ShowtimeRepository _repository;
  final MovieRepository _movieRepository;

  ShowtimeState _state = const ShowtimeState();
  ShowtimeState get state => _state;

  /// Thông tin phim cho header. Contract TV2 → TV3 chỉ truyền `movieId`,
  /// nên màn chọn suất tự tải chi tiết phim qua `MovieRepository`.
  Movie? _movie;
  Movie? get movie => _movie;

  int _requestNumber = 0;
  String? _movieRequestId;
  bool _disposed = false;

  Future<void> loadMovie(String movieId) async {
    if (_movie?.id == movieId || _movieRequestId == movieId) return;
    _movieRequestId = movieId;
    _movie = null;
    _notify();

    final result = await _movieRepository.getMovieDetail(movieId);
    if (_disposed || _movieRequestId != movieId) return;
    _movieRequestId = null;

    // Lỗi tải phim không chặn việc chọn suất; header dùng dữ liệu dự phòng.
    if (result case Success<Movie>(:final data)) {
      _movie = data;
      _notify();
    }
  }

  Future<void> load(String movieId, DateTime date) async {
    final requestNumber = ++_requestNumber;

    _state = const ShowtimeState(isLoading: true);
    _notify();

    final result = await _repository.getByMovieAndDate(
      movieId: movieId,
      date: date,
    );

    if (_disposed || requestNumber != _requestNumber) return;

    switch (result) {
      case Success<List<Showtime>>(:final data):
        _state = ShowtimeState(showtimes: data);
      case FailureResult<List<Showtime>>(:final failure):
        _state = ShowtimeState(errorMessage: failure.message);
    }

    _notify();
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
