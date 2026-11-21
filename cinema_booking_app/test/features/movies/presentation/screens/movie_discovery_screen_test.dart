import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie_schedule.dart';
import 'package:cinema_booking_app/features/movies/presentation/screens/movie_discovery_screen.dart';
import 'package:cinema_booking_app/features/movies/presentation/view_models/movie_discovery_view_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../watchlist/watchlist_test_fakes.dart';
import '../../movie_test_fakes.dart';

Widget _buildApp(
  FakeMovieRepository repository, {
  FakeWatchlistRepository? watchlist,
  bool signedIn = true,
}) {
  final router = GoRouter(
    initialLocation: '/movies',
    routes: [
      GoRoute(
        path: '/movies',
        builder: (context, state) => const MovieDiscoveryScreen(),
        routes: [
          GoRoute(
            path: ':movieId',
            builder: (context, state) =>
                Text('detail:${state.pathParameters['movieId']}'),
          ),
        ],
      ),
    ],
  );
  return MultiProvider(
    providers: [
      ChangeNotifierProvider(
        create: (_) => MovieDiscoveryViewModel(repository)..load(),
      ),
      ChangeNotifierProvider(
        create: (_) => buildWatchlistViewModel(
          watchlist ?? FakeWatchlistRepository(),
          signedIn: signedIn,
        ),
      ),
    ],
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  late FakeMovieRepository repository;

  setUp(() {
    repository = FakeMovieRepository()
      ..listResults[MovieSchedule.nowShowing] = ApiResult.success([
        buildMovie(genres: const ['Hành động']),
        buildMovie(
          id: 'MV000002',
          title: 'Inside Out 2',
          genres: const ['Hoạt hình'],
          ageRating: 'P',
        ),
      ])
      ..listResults[MovieSchedule.comingSoon] = ApiResult.success([
        buildMovie(id: 'MV000003', title: 'Avatar 3'),
      ]);
  });

  testWidgets('shows now-showing movies with genre chips', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    expect(find.text('Dune: Part Two'), findsOneWidget);
    expect(find.text('Inside Out 2'), findsOneWidget);
    expect(find.text('Tất cả'), findsOneWidget);
    expect(find.widgetWithText(ChoiceChip, 'Hoạt hình'), findsOneWidget);
    expect(find.text('T13'), findsOneWidget);
  });

  testWidgets('search and genre chip filter the grid', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'dune');
    await tester.pump();
    expect(find.text('Dune: Part Two'), findsOneWidget);
    expect(find.text('Inside Out 2'), findsNothing);

    final animationChip = find.widgetWithText(ChoiceChip, 'Hoạt hình');
    await tester.ensureVisible(animationChip);
    await tester.pumpAndSettle();
    await tester.tap(animationChip);
    await tester.pump();
    expect(find.text('Không tìm thấy phim phù hợp.'), findsOneWidget);
  });

  testWidgets('coming-soon tab loads its own list', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    await tester.tap(find.text('SẮP CHIẾU'));
    await tester.pumpAndSettle();

    expect(find.text('Avatar 3'), findsOneWidget);
    expect(find.text('Dune: Part Two'), findsNothing);
  });

  testWidgets('tapping a card opens detail route with movieId', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Inside Out 2'));
    await tester.pumpAndSettle();

    expect(find.text('detail:MV000002'), findsOneWidget);
  });

  testWidgets('shows error with retry that reloads', (tester) async {
    repository.listResults[MovieSchedule.nowShowing] = ApiResult.failure(
      const Failure('Không thể kết nối máy chủ. Vui lòng thử lại.'),
    );
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    expect(
      find.text('Không thể kết nối máy chủ. Vui lòng thử lại.'),
      findsOneWidget,
    );

    repository.listResults[MovieSchedule.nowShowing] = ApiResult.success([
      buildMovie(),
    ]);
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();

    expect(find.text('Dune: Part Two'), findsOneWidget);
  });

  testWidgets('shows empty message when schedule has no movies', (
    tester,
  ) async {
    repository.listResults[MovieSchedule.nowShowing] = ApiResult.success(
      const [],
    );
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    expect(find.text('Hiện chưa có phim đang chiếu.'), findsOneWidget);
  });

  testWidgets('saved tab lists the movies in the watchlist', (tester) async {
    final watchlist = FakeWatchlistRepository()
      ..saved.add(buildMovie(id: 'MV000009', title: 'Phim đã lưu'));
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, watchlist: watchlist));
    await tester.pumpAndSettle();

    await tester.tap(find.text('MỤC YÊU THÍCH'));
    await tester.pumpAndSettle();

    expect(find.text('Phim đã lưu'), findsOneWidget);
    expect(find.text('Inside Out 2'), findsNothing);
  });

  testWidgets('saved tab shows an empty message', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository));
    await tester.pumpAndSettle();

    await tester.tap(find.text('MỤC YÊU THÍCH'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Bạn chưa lưu phim nào'), findsOneWidget);
  });

  testWidgets('removing a movie on the saved tab drops it from the grid', (
    tester,
  ) async {
    final watchlist = FakeWatchlistRepository()
      ..saved.add(buildMovie(id: 'MV000009', title: 'Phim đã lưu'));
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, watchlist: watchlist));
    await tester.pumpAndSettle();
    await tester.tap(find.text('MỤC YÊU THÍCH'));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Bỏ khỏi watchlist'));
    await tester.pumpAndSettle();

    expect(find.text('Phim đã lưu'), findsNothing);
    expect(watchlist.removed, ['MV000009']);
  });

  testWidgets('saved tab asks a guest to sign in', (tester) async {
    usePhoneViewport(tester);
    await tester.pumpWidget(_buildApp(repository, signedIn: false));
    await tester.pumpAndSettle();

    await tester.tap(find.text('MỤC YÊU THÍCH'));
    await tester.pumpAndSettle();

    expect(find.text('Đăng nhập để xem các phim đã lưu.'), findsOneWidget);
  });
}
