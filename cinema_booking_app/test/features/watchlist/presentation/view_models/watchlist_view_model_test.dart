import 'dart:async';

import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/watchlist/presentation/view_models/watchlist_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../movies/movie_test_fakes.dart';
import '../../watchlist_test_fakes.dart';

void main() {
  late FakeWatchlistRepository repository;

  setUp(() => repository = FakeWatchlistRepository());

  Future<WatchlistViewModel> signedIn() async {
    final viewModel = buildWatchlistViewModel(repository);
    await Future<void>.delayed(Duration.zero);
    return viewModel;
  }

  test('loads saved movies when the session starts', () async {
    repository.saved.add(buildMovie(id: 'MV000001'));
    final viewModel = await signedIn();

    expect(viewModel.contains('MV000001'), isTrue);
    expect(viewModel.contains('MV000002'), isFalse);
  });

  test(
    'savedMovies drops a removed movie at once and adds after reload',
    () async {
      repository.saved.addAll([
        buildMovie(id: 'MV000001', title: 'A'),
        buildMovie(id: 'MV000002', title: 'B'),
      ]);
      final viewModel = await signedIn();
      expect(viewModel.savedMovies.map((m) => m.id), ['MV000001', 'MV000002']);

      await viewModel.toggle('MV000001');
      expect(viewModel.savedMovies.map((m) => m.id), ['MV000002']);

      await viewModel.toggle('MV000003');
      expect(viewModel.savedMovies.map((m) => m.id), ['MV000003', 'MV000002']);
    },
  );

  test('a failed load keeps the movies already shown', () async {
    repository.saved.add(buildMovie(id: 'MV000001'));
    final viewModel = await signedIn();

    repository.listFailure = ApiResult.failure(const Failure('Mất mạng'));
    await viewModel.load();

    expect(viewModel.status, WatchlistLoadStatus.failure);
    expect(viewModel.loadError, 'Mất mạng');
    expect(viewModel.savedMovies.map((m) => m.id), ['MV000001']);
  });

  test('toggle adds an unsaved movie and removes a saved one', () async {
    final viewModel = await signedIn();

    expect(await viewModel.toggle('MV000001'), WatchlistToggleOutcome.added);
    expect(viewModel.contains('MV000001'), isTrue);
    expect(repository.added, ['MV000001']);

    expect(await viewModel.toggle('MV000001'), WatchlistToggleOutcome.removed);
    expect(viewModel.contains('MV000001'), isFalse);
    expect(repository.removed, ['MV000001']);
  });

  test('failed write rolls the change back and keeps the message', () async {
    repository.writeResult = ApiResult.failure(watchlistFailure);
    final viewModel = await signedIn();

    expect(await viewModel.toggle('MV000001'), WatchlistToggleOutcome.failed);
    expect(viewModel.contains('MV000001'), isFalse);
    expect(viewModel.errorMessage, watchlistFailure.message);
  });

  test('failed removal restores the saved movie', () async {
    repository.saved.add(buildMovie(id: 'MV000001'));
    final viewModel = await signedIn();
    repository.writeResult = ApiResult.failure(const Failure('Lỗi mạng'));

    expect(await viewModel.toggle('MV000001'), WatchlistToggleOutcome.failed);
    expect(viewModel.contains('MV000001'), isTrue);
  });

  test('ignores a second tap while the first is pending', () async {
    final viewModel = await signedIn();
    repository.pendingWrite = Completer<ApiResult<void>>();

    final first = viewModel.toggle('MV000001');
    expect(viewModel.isPending('MV000001'), isTrue);
    expect(await viewModel.toggle('MV000001'), WatchlistToggleOutcome.ignored);

    repository.pendingWrite!.complete(ApiResult.success(null));
    expect(await first, WatchlistToggleOutcome.added);
    expect(viewModel.isPending('MV000001'), isFalse);
    expect(repository.added, ['MV000001']);
  });

  test('requires sign in', () async {
    final viewModel = buildWatchlistViewModel(repository, signedIn: false);
    await Future<void>.delayed(Duration.zero);

    expect(
      await viewModel.toggle('MV000001'),
      WatchlistToggleOutcome.signInRequired,
    );
    expect(repository.added, isEmpty);
  });

  test('logging out clears the saved movies', () async {
    repository.saved.add(buildMovie(id: 'MV000001'));
    final viewModel = await signedIn();

    viewModel.syncSession(null);
    await Future<void>.delayed(Duration.zero);

    expect(viewModel.contains('MV000001'), isFalse);
  });
}
