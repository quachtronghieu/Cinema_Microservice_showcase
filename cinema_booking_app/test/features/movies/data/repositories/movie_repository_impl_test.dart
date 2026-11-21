import 'package:cinema_booking_app/core/config/api_config.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/data/repositories/movie_repository_impl.dart';
import 'package:cinema_booking_app/features/movies/data/services/movie_api_service.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie_schedule.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeMovieApiService extends MovieApiService {
  _FakeMovieApiService() : super(dio: Dio());

  /// Trang trả về theo thứ tự `page`.
  List<dynamic> listResponses = const [];
  dynamic detailResponse;
  DioException? exceptionToThrow;
  MovieSchedule? requestedSchedule;
  String? requestedSearch;
  final List<int> requestedPages = [];
  String? requestedMovieId;

  @override
  Future<Response<dynamic>> getMovies(
    MovieSchedule schedule, {
    String? search,
    int page = 0,
  }) async {
    requestedSchedule = schedule;
    requestedSearch = search;
    requestedPages.add(page);
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: ApiConfig.movies),
      statusCode: 200,
      data: listResponses[page],
    );
  }

  @override
  Future<Response<dynamic>> getMovieDetail(String movieId) async {
    requestedMovieId = movieId;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: ApiConfig.movieDetail(movieId)),
      statusCode: 200,
      data: detailResponse,
    );
  }
}

DioException _httpError(int statusCode, {dynamic data}) {
  final options = RequestOptions(path: ApiConfig.movies);
  return DioException(
    requestOptions: options,
    response: Response(
      requestOptions: options,
      statusCode: statusCode,
      data: data,
    ),
  );
}

const _movieJson = {
  'movieId': 'MV000001',
  'title': 'Dune: Part Two',
  'genre': 'Khoa học viễn tưởng, Phiêu lưu',
  'summary': '  Paul Atreides hợp lực cùng Chani.  ',
  'duration': 166,
  'releaseDate': '2026-09-12',
  'ageRating': 'T13',
  'img': '/assets/img/movies/dune.jpg',
  'trailer': '',
  'active': true,
  'version': 0,
};

Map<String, dynamic> _page(
  List<Map<String, dynamic>> items, {
  int page = 0,
  int totalPages = 1,
}) => {
  'items': items,
  'page': page,
  'size': MovieApiService.pageSize,
  'totalElements': items.length,
  'totalPages': totalPages,
};

void main() {
  late _FakeMovieApiService api;
  late MovieRepositoryImpl repository;

  setUp(() {
    api = _FakeMovieApiService();
    repository = MovieRepositoryImpl(api);
  });

  group('getMovies', () {
    test('maps backend page to entities', () async {
      api.listResponses = [
        _page([_movieJson]),
      ];

      final result = await repository.getMovies(MovieSchedule.comingSoon);

      expect(api.requestedSchedule, MovieSchedule.comingSoon);
      expect(result, isA<Success<List<Movie>>>());
      final movie = (result as Success<List<Movie>>).data.single;
      expect(movie.id, 'MV000001');
      expect(movie.genres, ['Khoa học viễn tưởng', 'Phiêu lưu']);
      expect(movie.genreLabel, 'Khoa học viễn tưởng · Phiêu lưu');
      expect(movie.summary, 'Paul Atreides hợp lực cùng Chani.');
      expect(movie.durationMinutes, 166);
      expect(movie.releaseDate, DateTime(2026, 9, 12));
      expect(movie.ageRating, 'T13');
      expect(
        movie.posterUrl,
        '${ApiConfig.baseUrl}/assets/img/movies/dune.jpg',
      );
      expect(movie.trailerUrl, isNull);
    });

    test('reads every page in order', () async {
      api.listResponses = [
        _page([_movieJson], totalPages: 2),
        _page(
          [
            {..._movieJson, 'movieId': 'MV000002', 'title': 'Arrival'},
          ],
          page: 1,
          totalPages: 2,
        ),
      ];

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      expect(api.requestedPages, [0, 1]);
      expect((result as Success<List<Movie>>).data.map((movie) => movie.id), [
        'MV000001',
        'MV000002',
      ]);
    });

    test('passes the search term to the server', () async {
      api.listResponses = [_page(const [])];

      final result = await repository.getMovies(
        MovieSchedule.nowShowing,
        search: 'dune',
      );

      expect(api.requestedSearch, 'dune');
      expect((result as Success<List<Movie>>).data, isEmpty);
    });

    test('rejects a movie repeated between pages', () async {
      api.listResponses = [
        _page([_movieJson], totalPages: 2),
        _page([_movieJson], page: 1, totalPages: 2),
      ];

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      expect(
        (result as FailureResult<List<Movie>>).failure.message,
        'Dữ liệu phim từ máy chủ không hợp lệ.',
      );
    });

    test('normalizes the trailer link stored by admin', () async {
      final cases = <String, String?>{
        ' https://youtu.be/abc ': 'https://youtu.be/abc',
        'youtu.be/abc': 'https://youtu.be/abc',
        '/assets/trailers/dune.mp4':
            '${ApiConfig.baseUrl}/assets/trailers/dune.mp4',
        'khong phai link': null,
        'abc123': null,
        '': null,
      };
      for (final entry in cases.entries) {
        api.listResponses = [
          _page([
            {..._movieJson, 'trailer': entry.key},
          ]),
        ];

        final result = await repository.getMovies(MovieSchedule.nowShowing);

        final movie = (result as Success<List<Movie>>).data.single;
        expect(movie.trailerUrl, entry.value, reason: 'trailer "${entry.key}"');
      }
    });

    test('accepts LocalDate serialized as array', () async {
      api.listResponses = [
        _page([
          {
            ..._movieJson,
            'releaseDate': [2026, 9, 2],
          },
        ]),
      ];

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      final movie = (result as Success<List<Movie>>).data.single;
      expect(movie.releaseDate, DateTime(2026, 9, 2));
    });

    test('returns connection failure when server is unreachable', () async {
      api.exceptionToThrow = DioException(
        requestOptions: RequestOptions(path: ApiConfig.movies),
        type: DioExceptionType.connectionError,
      );

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      expect(result, isA<FailureResult<List<Movie>>>());
      expect(
        (result as FailureResult<List<Movie>>).failure.message,
        'Không thể kết nối máy chủ tại ${ApiConfig.baseUrl}. '
        'Kiểm tra API_BASE_URL và kết nối mạng rồi thử lại.',
      );
    });

    test('uses backend message for 400 responses', () async {
      api.exceptionToThrow = _httpError(
        400,
        data: {'code': 'VALIDATION_ERROR', 'message': 'status không hợp lệ'},
      );

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      final failure = (result as FailureResult<List<Movie>>).failure;
      expect(failure.message, 'status không hợp lệ');
      expect(failure.statusCode, 400);
    });

    test('rejects payload that is not a page', () async {
      api.listResponses = [
        [_movieJson],
      ];

      final result = await repository.getMovies(MovieSchedule.nowShowing);

      expect(
        (result as FailureResult<List<Movie>>).failure.message,
        'Dữ liệu phim từ máy chủ không hợp lệ.',
      );
    });
  });

  group('getMovieDetail', () {
    test('maps a single movie', () async {
      api.detailResponse = _movieJson;

      final result = await repository.getMovieDetail('MV000001');

      expect(api.requestedMovieId, 'MV000001');
      expect((result as Success<Movie>).data.title, 'Dune: Part Two');
    });

    test('maps 404 to not-found failure', () async {
      api.exceptionToThrow = _httpError(404);

      final result = await repository.getMovieDetail('MV999999');

      final failure = (result as FailureResult<Movie>).failure;
      expect(failure.statusCode, 404);
      expect(failure.message, 'Không tìm thấy phim.');
    });
  });
}
