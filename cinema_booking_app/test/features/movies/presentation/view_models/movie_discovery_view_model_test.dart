import 'dart:async';

import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie.dart';
import 'package:cinema_booking_app/features/movies/domain/entities/movie_schedule.dart';
import 'package:cinema_booking_app/features/movies/presentation/states/movie_discovery_state.dart';
import 'package:cinema_booking_app/features/movies/presentation/view_models/movie_discovery_view_model.dart';
import 'package:fake_async/fake_async.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../movie_test_fakes.dart';

void main() {
  late FakeMovieRepository repository;
  late MovieDiscoveryViewModel vm;

  final dune = buildMovie();
  final inside = buildMovie(
    id: 'MV000002',
    title: 'Inside Out 2',
    genres: const ['Hoạt hình'],
    ageRating: 'P',
  );
  final upcoming = buildMovie(
    id: 'MV000003',
    title: 'Avatar 3',
    genres: const ['Phiêu lưu'],
  );

  setUp(() {
    repository = FakeMovieRepository()
      ..listResults[MovieSchedule.nowShowing] = ApiResult.success([
        dune,
        inside,
      ])
      ..listResults[MovieSchedule.comingSoon] = ApiResult.success([upcoming]);
    vm = MovieDiscoveryViewModel(repository);
  });

  tearDown(() => vm.dispose());

  test('load fetches now-showing movies by default', () async {
    await vm.load();

    expect(repository.requestedSchedules, [MovieSchedule.nowShowing]);
    expect(vm.state.status, MovieLoadStatus.success);
    expect(vm.visibleMovies, [dune, inside]);
    expect(vm.categories, ['Khoa học viễn tưởng', 'Phiêu lưu', 'Hoạt hình']);
  });

  test('filters by query and category together', () async {
    await vm.load();

    vm.updateQuery('  INSIDE ');
    expect(vm.visibleMovies, [inside]);

    vm.selectCategory('Phiêu lưu');
    expect(vm.visibleMovies, isEmpty);

    vm.updateQuery('');
    expect(vm.visibleMovies, [dune]);

    vm.selectCategory(null);
    expect(vm.visibleMovies, [dune, inside]);
  });

  group('server search', () {
    test('waits for the debounce and sends the trimmed term once', () {
      fakeAsync((async) {
        vm.load();
        async.flushMicrotasks();
        repository.requestedSearches.clear();

        vm.updateQuery('d');
        vm.updateQuery('du');
        async.elapse(MovieDiscoveryViewModel.searchDebounce ~/ 2);
        vm.updateQuery(' dune ');
        async.elapse(MovieDiscoveryViewModel.searchDebounce ~/ 2);
        expect(repository.requestedSearches, isEmpty);

        async.elapse(MovieDiscoveryViewModel.searchDebounce ~/ 2);
        expect(repository.requestedSearches, ['dune']);
      });
    });

    test('whitespace-only changes do not refetch', () {
      fakeAsync((async) {
        vm.load();
        async.flushMicrotasks();
        repository.requestedSearches.clear();

        vm.updateQuery('   ');
        async.elapse(MovieDiscoveryViewModel.searchDebounce * 2);

        expect(repository.requestedSearches, isEmpty);
        expect(vm.state.query, '   ');
      });
    });

    test('clearing the query reloads the full list', () {
      fakeAsync((async) {
        vm.updateQuery('dune');
        async.elapse(MovieDiscoveryViewModel.searchDebounce);
        repository.requestedSearches.clear();

        vm.updateQuery('');
        async.elapse(MovieDiscoveryViewModel.searchDebounce);

        expect(repository.requestedSearches, [null]);
      });
    });

    test('switching tab keeps the term and cancels the pending search', () {
      fakeAsync((async) {
        vm.updateQuery('avatar');
        vm.selectSchedule(MovieSchedule.comingSoon);
        async.elapse(MovieDiscoveryViewModel.searchDebounce * 2);

        expect(repository.requestedSchedules, [MovieSchedule.comingSoon]);
        expect(repository.requestedSearches, ['avatar']);
      });
    });

    test('ignores an older search that answers last', () async {
      final slow = Completer<ApiResult<List<Movie>>>();
      repository.pendingLists[MovieSchedule.nowShowing] = slow;
      final first = vm.load();

      repository.listResults[MovieSchedule.nowShowing] = ApiResult.success([
        inside,
      ]);
      await vm.load();
      slow.complete(ApiResult.success([dune]));
      await first;

      expect(vm.state.movies, [inside]);
    });

    test('dispose cancels the pending search', () {
      fakeAsync((async) {
        final disposable = MovieDiscoveryViewModel(repository);
        disposable.updateQuery('dune');
        disposable.dispose();
        async.elapse(MovieDiscoveryViewModel.searchDebounce * 2);

        expect(repository.requestedSearches, isEmpty);
      });
    });
  });

  test('switching schedule resets category and loads that list', () async {
    await vm.load();
    vm.selectCategory('Hoạt hình');

    await vm.selectSchedule(MovieSchedule.comingSoon);

    expect(vm.state.schedule, MovieSchedule.comingSoon);
    expect(vm.state.category, isNull);
    expect(vm.visibleMovies, [upcoming]);
    expect(repository.requestedSchedules, [
      MovieSchedule.nowShowing,
      MovieSchedule.comingSoon,
    ]);
  });

  test('selecting the current schedule does not refetch', () async {
    await vm.load();

    await vm.selectSchedule(MovieSchedule.nowShowing);

    expect(repository.requestedSchedules, [MovieSchedule.nowShowing]);
  });

  test('ignores a stale response after switching tab quickly', () async {
    final slowNowShowing = Completer<ApiResult<List<Movie>>>();
    repository.pendingLists[MovieSchedule.nowShowing] = slowNowShowing;

    final firstLoad = vm.load();
    await vm.selectSchedule(MovieSchedule.comingSoon);
    slowNowShowing.complete(ApiResult.success([dune, inside]));
    await firstLoad;

    expect(vm.state.schedule, MovieSchedule.comingSoon);
    expect(vm.state.movies, [upcoming]);
  });

  test(
    'exposes failure message and keeps previous movies on refresh error',
    () async {
      await vm.load();
      repository.listResults[MovieSchedule.nowShowing] = ApiResult.failure(
        const Failure('Không thể kết nối máy chủ. Vui lòng thử lại.'),
      );

      await vm.load();

      expect(vm.state.status, MovieLoadStatus.failure);
      expect(
        vm.state.errorMessage,
        'Không thể kết nối máy chủ. Vui lòng thử lại.',
      );
      expect(vm.state.movies, [dune, inside]);
    },
  );

  test('does not notify after dispose', () async {
    final pending = Completer<ApiResult<List<Movie>>>();
    repository.pendingLists[MovieSchedule.nowShowing] = pending;
    final disposable = MovieDiscoveryViewModel(repository);

    final loading = disposable.load();
    disposable.dispose();
    pending.complete(ApiResult.success([dune]));

    await expectLater(loading, completes);
  });
}
