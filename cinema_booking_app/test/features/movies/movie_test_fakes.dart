import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/data/services/trailer_launcher.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie_schedule.dart';
import 'package:cinema_booking_app/features/movies/domain/repositories/movie_repository.dart';

/// Kích thước màn hình điện thoại (390x844) thay cho mặc định 800x600 của test.
void usePhoneViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(1170, 2532);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
}

Movie buildMovie({
  String id = 'MV000001',
  String title = 'Dune: Part Two',
  List<String> genres = const ['Khoa học viễn tưởng', 'Phiêu lưu'],
  String ageRating = 'T13',
  DateTime? releaseDate,
  String? trailerUrl,
}) => Movie(
  id: id,
  title: title,
  genres: genres,
  summary: 'Tóm tắt $title',
  durationMinutes: 120,
  ageRating: ageRating,
  releaseDate: releaseDate ?? DateTime(2026, 9, 12),
  trailerUrl: trailerUrl,
);

/// Repository giả; mỗi lời gọi có thể trả ngay hoặc chờ [Completer] để
/// kiểm tra thứ tự phản hồi.
class FakeMovieRepository implements MovieRepository {
  final Map<MovieSchedule, ApiResult<List<Movie>>> listResults = {
    MovieSchedule.nowShowing: ApiResult.success(const []),
    MovieSchedule.comingSoon: ApiResult.success(const []),
  };
  final Map<MovieSchedule, Completer<ApiResult<List<Movie>>>> pendingLists = {};
  ApiResult<Movie> detailResult = ApiResult.failure(
    const Failure('Không tìm thấy phim.', statusCode: 404),
  );
  final List<MovieSchedule> requestedSchedules = [];
  final List<String?> requestedSearches = [];
  final List<String> requestedMovieIds = [];

  @override
  Future<ApiResult<List<Movie>>> getMovies(
    MovieSchedule schedule, {
    String? search,
  }) {
    requestedSchedules.add(schedule);
    requestedSearches.add(search);
    final pending = pendingLists.remove(schedule);
    if (pending != null) return pending.future;
    return Future.value(listResults[schedule]!);
  }

  @override
  Future<ApiResult<Movie>> getMovieDetail(String movieId) async {
    requestedMovieIds.add(movieId);
    return detailResult;
  }
}

class FakeTrailerLauncher implements TrailerLauncher {
  bool result = true;
  final List<String> opened = [];

  @override
  Future<bool> open(String url) async {
    opened.add(url);
    return result;
  }
}
