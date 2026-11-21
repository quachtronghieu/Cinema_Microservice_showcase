import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/watchlist/data/repositories/watchlist_repository_impl.dart';
import 'package:cinema_booking_app/features/watchlist/data/services/watchlist_api_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

class _StubAdapter implements HttpClientAdapter {
  _StubAdapter(this.respond);

  final ResponseBody Function(RequestOptions options) respond;
  final List<RequestOptions> requests = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return respond(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody _json(String body, int status) => ResponseBody.fromString(
  body,
  status,
  headers: {
    Headers.contentTypeHeader: [Headers.jsonContentType],
  },
);

(WatchlistRepositoryImpl, _StubAdapter) _build(
  ResponseBody Function(RequestOptions) respond,
) {
  final adapter = _StubAdapter(respond);
  final dio = Dio(BaseOptions(baseUrl: 'http://test'))
    ..httpClientAdapter = adapter;
  return (WatchlistRepositoryImpl(WatchlistApiService(dio: dio)), adapter);
}

void main() {
  test('maps the watchlist response to movies in server order', () async {
    final (repository, _) = _build(
      (_) => _json(
        '[{"movieID":"MV000002","title":"B","genre":"Hành động"},'
        '{"movieID":"MV000001","title":"A"}]',
        200,
      ),
    );

    final result = await repository.getWatchlistMovies();

    final movies = (result as Success<List<Movie>>).data;
    expect(movies.map((movie) => movie.id), ['MV000002', 'MV000001']);
    expect(movies.first.title, 'B');
    expect(movies.first.genres, ['Hành động']);
  });

  test('add uses PUT and remove uses DELETE on the movie path', () async {
    final (repository, adapter) = _build((_) => _json('', 204));

    expect(await repository.add('MV000001'), isA<Success<void>>());
    expect(await repository.remove('MV000001'), isA<Success<void>>());

    expect(adapter.requests.map((r) => r.method), ['PUT', 'DELETE']);
    expect(
      adapter.requests.map((r) => r.path),
      everyElement('/api/v1/customer/watchlist/MV000001'),
    );
  });

  test('a redirect to the login page means the session expired', () async {
    final (repository, _) = _build(
      (_) => ResponseBody.fromString(
        '',
        302,
        headers: {
          Headers.contentTypeHeader: ['text/html'],
          'location': ['http://test/login'],
        },
      ),
    );

    final result = await repository.add('MV000001');

    final failure = (result as FailureResult<void>).failure;
    expect(failure.statusCode, 302);
    expect(failure.message, contains('đăng nhập lại'));
  });

  test('unknown movie maps to 404', () async {
    final (repository, _) = _build(
      (_) => _json('{"code":"NOT_FOUND","message":"x"}', 404),
    );

    final result = await repository.add('missing');

    final failure = (result as FailureResult<void>).failure;
    expect(failure.statusCode, 404);
    expect(failure.message, 'Không tìm thấy phim.');
  });

  test('missing backend route is not reported as a missing movie', () async {
    final (repository, _) = _build(
      (_) => _json('{"status":404,"error":"Not Found"}', 404),
    );

    final result = await repository.add('MV000001');

    final failure = (result as FailureResult<void>).failure;
    expect(failure.message, isNot('Không tìm thấy phim.'));
    expect(failure.message, contains('404'));
  });
}
