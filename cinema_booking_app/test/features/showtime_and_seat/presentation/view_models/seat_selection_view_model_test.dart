import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/seat_hold_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/seat_map_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/presentation/view_models/seat_selection_view_model.dart';
import 'package:fake_async/fake_async.dart';
import 'package:flutter_test/flutter_test.dart';

Seat seat(int number, {SeatStatus status = SeatStatus.available}) => Seat(
  showtimeSeatId: 'HS$number',
  templateSeatId: 'TS$number',
  rowLabel: 'A',
  seatNumber: number,
  code: 'A$number',
  type: SeatType.standard,
  status: status,
);

SeatMap seatMap(List<Seat> seats) =>
    SeatMap(showtimeId: 'SW1', movieTitle: 'DEMO', seats: seats);

class FakeSeatMaps implements SeatMapRepository {
  FakeSeatMaps(this.result);

  ApiResult<SeatMap> result;
  int loads = 0;

  @override
  Future<ApiResult<SeatMap>> getSeatMap(String showtimeId) async {
    loads++;
    return result;
  }
}

class FakeHolds implements SeatHoldRepository {
  ApiResult<SeatHold>? result;
  int holdCalls = 0;
  final released = <String>[];

  @override
  Future<ApiResult<SeatHold>> holdSeats({
    required String showtimeId,
    required List<String> seatIds,
  }) async {
    holdCalls++;
    return result!;
  }

  @override
  Future<ApiResult<void>> releaseHold(String holdId) async {
    released.add(holdId);
    return ApiResult.success(null);
  }
}

SeatHold activeHold(DateTime now, {String id = 'H1'}) => SeatHold(
  holdId: id,
  showtimeId: 'SW1',
  seatIds: const ['HS1', 'HS2'],
  expiresAt: now.add(const Duration(minutes: 5)),
  serverTime: now,
  deadline: now.add(const Duration(minutes: 5)),
);

void main() {
  final now = DateTime(2026, 11, 5, 15);
  final tenSeats = [for (var i = 1; i <= 10; i++) seat(i)];
  late FakeSeatMaps maps;
  late FakeHolds holds;
  late SeatSelectionViewModel vm;

  Future<void> loaded(List<Seat> seats) async {
    maps = FakeSeatMaps(ApiResult.success(seatMap(seats)));
    holds = FakeHolds();
    vm = SeatSelectionViewModel(maps, holds, now: () => now);
    await vm.load('SW1');
  }

  tearDown(() => vm.dispose());

  test(
    'selects and deselects a seat locally without calling the API',
    () async {
      await loaded(tenSeats);
      expect(vm.toggle(seat(1)), SeatToggleResult.selected);
      expect(vm.state.selectedSeatIds, {'HS1'});
      expect(vm.toggle(seat(1)), SeatToggleResult.deselected);
      expect(vm.state.selectedSeatIds, isEmpty);
      expect(holds.holdCalls, 0);
    },
  );

  test('stops at 8 seats', () async {
    await loaded(tenSeats);
    for (var i = 1; i <= 8; i++) {
      expect(vm.toggle(seat(i)), SeatToggleResult.selected);
    }
    expect(vm.toggle(seat(9)), SeatToggleResult.limitReached);
    expect(vm.state.selectedSeatIds, hasLength(8));
  });

  test('HELD, SOLD and unknown seats cannot be selected', () async {
    await loaded(tenSeats);
    for (final status in [
      SeatStatus.pending,
      SeatStatus.unavailable,
      SeatStatus.unknown,
    ]) {
      expect(vm.toggle(seat(1, status: status)), SeatToggleResult.unavailable);
    }
    expect(vm.state.selectedSeatIds, isEmpty);
  });

  test('row and adjacency rules are enforced on tap', () async {
    await loaded(tenSeats);
    vm.toggle(seat(2));
    expect(vm.toggle(seat(5)), SeatToggleResult.notAdjacent);
    final otherRow = Seat(
      showtimeSeatId: 'HSB',
      templateSeatId: 'TSB',
      rowLabel: 'B',
      seatNumber: 3,
      code: 'B3',
      type: SeatType.standard,
      status: SeatStatus.available,
    );
    expect(vm.toggle(otherRow), SeatToggleResult.differentRow);
  });

  test('a rule violation shows a message and never calls hold', () async {
    await loaded(tenSeats);
    vm.toggle(seat(2));
    vm.toggle(seat(3));
    final error = await vm.holdSelectedSeats();
    expect(error, contains('A1'));
    expect(holds.holdCalls, 0);
  });

  test(
    'a successful hold keeps the server hold; repeat does not call again',
    () async {
      await loaded(tenSeats);
      vm.toggle(seat(1));
      vm.toggle(seat(2));
      holds.result = ApiResult.success(activeHold(now));

      expect(await vm.holdSelectedSeats(), isNull);
      expect(vm.state.hold?.holdId, 'H1');
      expect(await vm.holdSelectedSeats(), isNull);
      expect(holds.holdCalls, 1);

      // Released when leaving the screen.
      vm.dispose();
      expect(holds.released, ['H1']);
      await loaded(tenSeats);
    },
  );

  test('409 with seatIds names the seats and reloads availability', () async {
    await loaded(tenSeats);
    vm.toggle(seat(1));
    vm.toggle(seat(2));
    holds.result = ApiResult.failure(
      const Failure(
        'Ghế không còn trống.',
        statusCode: 409,
        errors: {
          'code': 'SEAT_UNAVAILABLE',
          'seatIds': ['HS2'],
        },
      ),
    );
    maps.result = ApiResult.success(
      seatMap([seat(1), seat(2, status: SeatStatus.pending), seat(3)]),
    );

    final error = await vm.holdSelectedSeats();
    await Future<void>.delayed(Duration.zero);

    expect(error, contains('A2'));
    expect(maps.loads, 2);
    expect(vm.state.selectedSeatIds, {'HS1'});
  });

  test('409 without seatIds still reloads availability', () async {
    await loaded(tenSeats);
    vm.toggle(seat(1));
    vm.toggle(seat(2));
    holds.result = ApiResult.failure(
      const Failure(
        'Suất chiếu đã đóng bán.',
        statusCode: 409,
        errors: {'code': 'SALES_CLOSED'},
      ),
    );

    final error = await vm.holdSelectedSeats();
    await Future<void>.delayed(Duration.zero);

    expect(error, 'Suất chiếu đã đóng bán.');
    expect(maps.loads, 2);
  });

  test('other failures keep the current map without reloading', () async {
    await loaded(tenSeats);
    vm.toggle(seat(1));
    vm.toggle(seat(2));
    holds.result = ApiResult.failure(
      const Failure('Vui lòng đăng nhập.', statusCode: 401),
    );

    expect(await vm.holdSelectedSeats(), 'Vui lòng đăng nhập.');
    await Future<void>.delayed(Duration.zero);
    expect(maps.loads, 1);
  });

  test('a load failure reports an error, never free seats', () async {
    await loaded(tenSeats);
    maps.result = ApiResult.failure(
      const Failure('Không thể tải.', statusCode: 503),
    );
    await vm.load('SW1');

    expect(vm.state.errorMessage, 'Không thể tải.');
    expect(vm.state.seatMap?.seats, hasLength(10));
  });

  test('when the hold expires, it clears, releases and reloads', () {
    fakeAsync((async) {
      maps = FakeSeatMaps(ApiResult.success(seatMap(tenSeats)));
      holds = FakeHolds()..result = ApiResult.success(activeHold(now));
      vm = SeatSelectionViewModel(maps, holds, now: () => now);
      vm.load('SW1');
      async.flushMicrotasks();
      vm.toggle(seat(1));
      vm.toggle(seat(2));
      vm.holdSelectedSeats();
      async.flushMicrotasks();
      expect(vm.state.hold, isNotNull);

      async.elapse(const Duration(minutes: 5));
      async.flushMicrotasks();

      expect(vm.state.hold, isNull);
      expect(vm.state.holdExpired, isTrue);
      expect(holds.released, ['H1']);
      expect(maps.loads, 2);
      expect(vm.state.selectedSeatIds, {'HS1', 'HS2'});
    });
  });
}
