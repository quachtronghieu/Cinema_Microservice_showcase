import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/data/services/trailer_launcher.dart';
import 'package:cinema_booking_app/features/movies/domain/repositories/movie_repository.dart';
import 'package:cinema_booking_app/features/movies/presentation/screens/movie_detail_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../watchlist/watchlist_test_fakes.dart';
import '../../movie_test_fakes.dart';

Widget _buildApp(
  FakeMovieRepository repository,
  String movieId, {
  FakeWatchlistRepository? watchlist,
  FakeTrailerLauncher? trailerLauncher,
}) {
  final watchlistRepository = watchlist ?? FakeWatchlistRepository();
  final router = GoRouter(
    initialLocation: '/movies/$movieId',
    routes: [
      GoRoute(
        path: '/movies',
        builder: (context, state) => const Text('movie list'),
        routes: [
          GoRoute(
            path: ':movieId',
            builder: (context, state) =>
                MovieDetailScreen(movieId: state.pathParameters['movieId']!),
          ),
        ],
      ),
      GoRoute(
        path: '/showtimes/:movieId',
        builder: (context, state) =>
            Text('showtimes:${state.pathParameters['movieId']}'),
      ),
    ],
  );
  return MultiProvider(
    providers: [
      Provider<MovieRepository>.value(value: repository),
      Provider<TrailerLauncher>.value(
        value: trailerLauncher ?? FakeTrailerLauncher(),
      ),
      ChangeNotifierProvider(
        create: (_) => buildWatchlistViewModel(watchlistRepository),
      ),
    ],
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  late FakeMovieRepository repository;

  setUp(() => repository = FakeMovieRepository());

  testWidgets('renders movie information from repository', (tester) async {
    repository.detailResult = ApiResult.success(
      buildMovie(releaseDate: DateTime(2026, 9, 2)),
    );

    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, 'MV000001'));
    await tester.pumpAndSettle();

    expect(repository.requestedMovieIds, ['MV000001']);
    expect(find.text('DUNE: PART TWO'), findsOneWidget);
    expect(find.text('120 phút'), findsOneWidget);
    expect(find.text('Khoa học viễn tưởng · Phiêu lưu'), findsOneWidget);
    expect(find.text('Tóm tắt Dune: Part Two'), findsOneWidget);
    expect(find.text('02/09/2026'), findsOneWidget);
    expect(find.text('T13'), findsNWidgets(2));
  });

  testWidgets('select showtime passes movieId to showtime route', (
    tester,
  ) async {
    repository.detailResult = ApiResult.success(buildMovie(id: 'MV000009'));

    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, 'MV000009'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chọn suất chiếu'));
    await tester.pumpAndSettle();

    expect(find.text('showtimes:MV000009'), findsOneWidget);
  });

  testWidgets('bookmark button adds and removes the movie from watchlist', (
    tester,
  ) async {
    final watchlist = FakeWatchlistRepository();
    repository.detailResult = ApiResult.success(buildMovie(id: 'MV000009'));

    usePhoneViewport(tester);
    await tester.pumpWidget(
      _buildApp(repository, 'MV000009', watchlist: watchlist),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Thêm vào watchlist'));
    await tester.pumpAndSettle();
    expect(watchlist.added, ['MV000009']);
    expect(find.byTooltip('Bỏ khỏi watchlist'), findsOneWidget);

    await tester.tap(find.byTooltip('Bỏ khỏi watchlist'));
    await tester.pumpAndSettle();
    expect(watchlist.removed, ['MV000009']);
    expect(find.byTooltip('Thêm vào watchlist'), findsOneWidget);
  });

  testWidgets('trailer button opens the stored trailer link', (tester) async {
    final launcher = FakeTrailerLauncher();
    repository.detailResult = ApiResult.success(
      buildMovie(trailerUrl: 'https://youtu.be/abc123'),
    );

    usePhoneViewport(tester);
    await tester.pumpWidget(
      _buildApp(repository, 'MV000001', trailerLauncher: launcher),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem trailer'));
    await tester.pumpAndSettle();

    expect(launcher.opened, ['https://youtu.be/abc123']);
  });

  testWidgets('trailer that cannot be opened shows an error', (tester) async {
    final launcher = FakeTrailerLauncher()..result = false;
    repository.detailResult = ApiResult.success(
      buildMovie(trailerUrl: 'https://youtu.be/abc123'),
    );

    usePhoneViewport(tester);
    await tester.pumpWidget(
      _buildApp(repository, 'MV000001', trailerLauncher: launcher),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem trailer'));
    await tester.pumpAndSettle();

    expect(
      find.text('Không mở được trailer. Vui lòng thử lại sau.'),
      findsOneWidget,
    );
  });

  testWidgets('movie without trailer disables the button', (tester) async {
    final launcher = FakeTrailerLauncher();
    repository.detailResult = ApiResult.success(buildMovie());

    usePhoneViewport(tester);
    await tester.pumpWidget(
      _buildApp(repository, 'MV000001', trailerLauncher: launcher),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chưa có trailer'));
    await tester.pumpAndSettle();

    expect(launcher.opened, isEmpty);
  });

  testWidgets('not found hides retry', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, 'missing'));
    await tester.pumpAndSettle();

    expect(find.text('Không tìm thấy phim.'), findsOneWidget);
    expect(find.text('Retry'), findsNothing);
  });

  testWidgets('connection error can be retried', (tester) async {
    repository.detailResult = ApiResult.failure(
      const Failure('Không thể kết nối máy chủ. Vui lòng thử lại.'),
    );
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, 'MV000001'));
    await tester.pumpAndSettle();

    repository.detailResult = ApiResult.success(buildMovie());
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();

    expect(find.text('DUNE: PART TWO'), findsOneWidget);
    expect(repository.requestedMovieIds, ['MV000001', 'MV000001']);
  });

  testWidgets('back without history returns to movie list', (tester) async {
    repository.detailResult = ApiResult.success(buildMovie());
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, 'MV000001'));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Quay lại'));
    await tester.pumpAndSettle();

    expect(find.text('movie list'), findsOneWidget);
  });
}
