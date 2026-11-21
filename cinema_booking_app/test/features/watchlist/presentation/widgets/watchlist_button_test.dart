import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/watchlist/presentation/view_models/watchlist_view_model.dart';
import 'package:cinema_booking_app/features/watchlist/presentation/widgets/watchlist_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../watchlist_test_fakes.dart';

Widget _buildApp(WatchlistViewModel viewModel) {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) =>
            const Scaffold(body: WatchlistButton(movieId: 'MV000001')),
      ),
      GoRoute(path: '/login', builder: (context, state) => const Text('login')),
    ],
  );
  return ChangeNotifierProvider.value(
    value: viewModel,
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  late FakeWatchlistRepository repository;

  setUp(() => repository = FakeWatchlistRepository());

  testWidgets('tap adds then removes the movie', (tester) async {
    await tester.pumpWidget(_buildApp(buildWatchlistViewModel(repository)));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.bookmark_border), findsOneWidget);

    await tester.tap(find.byType(IconButton));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.bookmark), findsOneWidget);
    expect(find.text('Đã thêm vào watchlist.'), findsOneWidget);

    await tester.tap(find.byType(IconButton));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.bookmark_border), findsOneWidget);
    expect(find.text('Đã bỏ khỏi watchlist.'), findsOneWidget);
    expect(repository.added, ['MV000001']);
    expect(repository.removed, ['MV000001']);
  });

  testWidgets('shows the server error and keeps the icon unchanged', (
    tester,
  ) async {
    repository.writeResult = ApiResult.failure(watchlistFailure);
    await tester.pumpWidget(_buildApp(buildWatchlistViewModel(repository)));
    await tester.pumpAndSettle();

    await tester.tap(find.byType(IconButton));
    await tester.pumpAndSettle();

    expect(find.text(watchlistFailure.message), findsOneWidget);
    expect(find.byIcon(Icons.bookmark_border), findsOneWidget);
  });

  testWidgets('guest is sent to the login screen', (tester) async {
    await tester.pumpWidget(
      _buildApp(buildWatchlistViewModel(repository, signedIn: false)),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byType(IconButton));
    await tester.pumpAndSettle();

    expect(find.text('login'), findsOneWidget);
    expect(repository.added, isEmpty);
  });
}
