import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/movies/presentation/states/movie_discovery_state.dart';
import 'package:cinema_booking_app/features/movies/presentation/view_models/movie_detail_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../movie_test_fakes.dart';

void main() {
  late FakeMovieRepository repository;

  setUp(() => repository = FakeMovieRepository());

  test('loads movie by id', () async {
    final movie = buildMovie(id: 'MV000007');
    repository.detailResult = ApiResult.success(movie);
    final vm = MovieDetailViewModel(repository, movieId: 'MV000007');

    await vm.load();

    expect(repository.requestedMovieIds, ['MV000007']);
    expect(vm.state.status, MovieLoadStatus.success);
    expect(vm.state.movie, movie);
    vm.dispose();
  });

  test('marks 404 as not found', () async {
    final vm = MovieDetailViewModel(repository, movieId: 'missing');

    await vm.load();

    expect(vm.state.status, MovieLoadStatus.failure);
    expect(vm.state.notFound, isTrue);
    expect(vm.state.errorMessage, 'Không tìm thấy phim.');
    vm.dispose();
  });

  test('other failures are retryable', () async {
    repository.detailResult = ApiResult.failure(
      const Failure('Không thể kết nối máy chủ. Vui lòng thử lại.'),
    );
    final vm = MovieDetailViewModel(repository, movieId: 'MV000001');

    await vm.load();

    expect(vm.state.notFound, isFalse);
    expect(vm.state.status, MovieLoadStatus.failure);
    vm.dispose();
  });
}
