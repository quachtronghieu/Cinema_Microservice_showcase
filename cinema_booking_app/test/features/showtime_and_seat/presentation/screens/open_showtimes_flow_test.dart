import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/data/services/trailer_launcher.dart';
import 'package:cinema_booking_app/features/movies/domain/repositories/movie_repository.dart';
import 'package:cinema_booking_app/features/movies/presentation/screens/movie_detail_screen.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/repositories/showtime_repository_impl.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/services/showtime_api_service.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/showtime.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/showtime_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/presentation/screens/showtime_screen.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/presentation/view_models/showtime_view_model.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../movies/movie_test_fakes.dart';
import '../../../watchlist/watchlist_test_fakes.dart';

class _FakeShowtimeRepository implements ShowtimeRepository {
  ApiResult<List<Showtime>> result = ApiResult.success(const []);
  final List<String> requestedMovieIds = [];
  final List<DateTime> requestedDates = [];

  @override
  Future<ApiResult<List<Showtime>>> getByMovieAndDate({
    required String movieId,
    required DateTime date,
  }) async {
    requestedMovieIds.add(movieId);
    requestedDates.add(date);
    return result;
  }
}

Showtime _showtime() => Showtime(
  showtimeId: 'ST000001',
  movieId: 'MV000009',
  movieTitle: 'DUNE',
  showDate: DateUtils.dateOnly(DateTime.now()),
  startTime: '19:30:00',
  endTime: '21:30:00',
);

Widget _buildApp(
  FakeMovieRepository movies,
  _FakeShowtimeRepository showtimes,
) {
  final router = GoRouter(
    initialLocation: '/movies/MV000009',
    routes: [
      GoRoute(
        path: '/movies/:movieId',
        builder: (context, state) =>
            MovieDetailScreen(movieId: state.pathParameters['movieId']!),
      ),
      // Cùng cách dựng với route `/showtimes/:movieId` trong app_router.
      GoRoute(
        path: '/showtimes/:movieId',
        builder: (context, state) {
          final movieId = state.pathParameters['movieId']!;
          return ChangeNotifierProvider<ShowtimeViewModel>(
            create: (context) =>
                ShowtimeViewModel(
                    context.read<ShowtimeRepository>(),
                    context.read<MovieRepository>(),
                  )
                  ..loadMovie(movieId)
                  ..load(movieId, DateUtils.dateOnly(DateTime.now())),
            child: ShowtimeSelectionScreen(movieId: movieId),
          );
        },
      ),
      GoRoute(
        path: '/seats/:showtimeId',
        builder: (context, state) =>
            Text('seats:${state.pathParameters['showtimeId']}'),
      ),
    ],
  );
  return MultiProvider(
    providers: [
      Provider<MovieRepository>.value(value: movies),
      Provider<ShowtimeRepository>.value(value: showtimes),
      Provider<TrailerLauncher>.value(value: FakeTrailerLauncher()),
      ChangeNotifierProvider(
        create: (_) => buildWatchlistViewModel(FakeWatchlistRepository()),
      ),
    ],
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  late FakeMovieRepository movies;
  late _FakeShowtimeRepository showtimes;

  setUp(() {
    movies = FakeMovieRepository()
      ..detailResult = ApiResult.success(
        buildMovie(id: 'MV000009', title: 'Dune'),
      );
    showtimes = _FakeShowtimeRepository();
  });

  Future<void> openShowtimes(WidgetTester tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(movies, showtimes));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chọn suất chiếu'));
    await tester.pumpAndSettle();
  }

  testWidgets('selected movie opens its showtimes for today', (tester) async {
    showtimes.result = ApiResult.success([_showtime()]);

    await openShowtimes(tester);

    expect(showtimes.requestedMovieIds, ['MV000009']);
    expect(showtimes.requestedDates, [DateUtils.dateOnly(DateTime.now())]);
    expect(find.text('Dune'), findsOneWidget);
    expect(find.text('19:30 - 21:30'), findsOneWidget);
  });

  testWidgets('choosing another date reloads the same movie', (tester) async {
    await openShowtimes(tester);

    final tomorrow = DateUtils.dateOnly(
      DateTime.now().add(const Duration(days: 1)),
    );
    await tester.tap(find.text(tomorrow.day.toString().padLeft(2, '0')));
    await tester.pumpAndSettle();

    expect(showtimes.requestedMovieIds, ['MV000009', 'MV000009']);
    expect(showtimes.requestedDates.last, tomorrow);
  });

  testWidgets('showtime passes only its id to the seat map', (tester) async {
    showtimes.result = ApiResult.success([_showtime()]);

    await openShowtimes(tester);
    await tester.tap(find.text('19:30 - 21:30'));
    await tester.pumpAndSettle();

    expect(find.text('seats:ST000001'), findsOneWidget);
  });

  testWidgets('movie without showtimes shows the empty state', (tester) async {
    await openShowtimes(tester);

    expect(find.text('Chưa có suất chiếu trong ngày này.'), findsOneWidget);
  });

  testWidgets('unavailable cinema service can be retried', (tester) async {
    showtimes.result = ApiResult.failure(
      const Failure('Service temporarily unavailable', statusCode: 503),
    );

    await openShowtimes(tester);
    expect(find.text('Service temporarily unavailable'), findsOneWidget);

    showtimes.result = ApiResult.success([_showtime()]);
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();

    expect(find.text('19:30 - 21:30'), findsOneWidget);
  });

  test('showtime request sends the movie id and formatted date', () async {
    late RequestOptions sent;
    final dio = Dio()
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            sent = options;
            handler.resolve(Response(requestOptions: options, data: const []));
          },
        ),
      );

    await ShowtimeApiService(dio: dio)
        .getByMovieAndDate(movieId: 'a/b', date: DateTime(2026, 11, 4));

    expect(sent.path, '/api/v1/showtimes');
    expect(sent.queryParameters, {'movieId': 'a/b', 'date': '2026-11-04'});
  });

  test('repository maps the cinema-service response', () async {
    final dio = Dio()
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) => handler.resolve(
            Response(
              requestOptions: options,
              data: const [
                {
                  'showtimeId': 'ST000001',
                  'movieId': 'a563f4f6-e6aa-49ae-b0a1-7e22ee6e4325',
                  'movieTitle': 'THE ODYSSEY',
                  'roomId': 'R1',
                  'showDate': '2026-11-04',
                  'startTime': '19:30:00',
                  'endTime': '22:15:00',
                  'status': null,
                },
              ],
            ),
          ),
        ),
      );

    final result = await ShowtimeRepositoryImpl(ShowtimeApiService(dio: dio))
        .getByMovieAndDate(movieId: 'x', date: DateTime(2026, 11, 4));

    final showtime = (result as Success<List<Showtime>>).data.single;
    expect(showtime.showtimeId, 'ST000001');
    expect(showtime.movieTitle, 'THE ODYSSEY');
    expect(showtime.roomId, 'R1');
    expect(showtime.startTime, '19:30:00');
  });
}
