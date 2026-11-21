import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/use_cases/validate_seat_selection.dart';
import 'package:flutter_test/flutter_test.dart';

Seat seat(String row, int number, {SeatStatus status = SeatStatus.available}) =>
    Seat(
      showtimeSeatId: '$row$number',
      templateSeatId: 'T$row$number',
      rowLabel: row,
      seatNumber: number,
      code: '$row$number',
      type: SeatType.standard,
      status: status,
    );

void main() {
  const validate = ValidateSeatSelection();
  final row = [for (var i = 1; i <= 6; i++) seat('A', i)];

  test('accepts an empty selection', () {
    expect(validate(seats: row, selectedSeatIds: const {}), isNull);
  });

  test('rejects seats in different rows', () {
    final violation = validate(
      seats: [...row, seat('B', 1)],
      selectedSeatIds: const {'A1', 'B1'},
    );
    expect(violation?.type, SeatSelectionViolationType.differentRow);
  });

  test('rejects seats that are not contiguous', () {
    final violation = validate(seats: row, selectedSeatIds: const {'A1', 'A3'});
    expect(violation?.type, SeatSelectionViolationType.notContiguous);
  });

  test('rejects a selection leaving one free seat at the row edge', () {
    final violation = validate(seats: row, selectedSeatIds: const {'A2', 'A3'});
    expect(violation?.type, SeatSelectionViolationType.leavesSingleGap);
    expect(violation?.seatCodes, ['A1']);
  });

  test('rejects a single gap next to a seat someone else holds', () {
    final seats = [
      for (var i = 1; i <= 6; i++)
        seat(
          'A',
          i,
          status: i == 1 ? SeatStatus.pending : SeatStatus.available,
        ),
    ];
    final violation = validate(
      seats: seats,
      selectedSeatIds: const {'A3', 'A4'},
    );
    expect(violation?.seatCodes, ['A2']);
  });

  test('accepts contiguous seats that leave two free seats', () {
    expect(validate(seats: row, selectedSeatIds: const {'A3', 'A4'}), isNull);
  });

  test('seats I hold but no longer select count as free', () {
    final seats = [
      for (var i = 1; i <= 6; i++)
        seat(
          'A',
          i,
          status: i == 1 ? SeatStatus.pending : SeatStatus.available,
        ),
    ];
    expect(
      validate(
        seats: seats,
        selectedSeatIds: const {'A3', 'A4'},
        ownHeldSeatIds: const {'A1'},
      ),
      isNull,
    );
  });

  test('checkAddition allows a temporary gap but not a distant seat', () {
    expect(
      validate.checkAddition(seat: seat('A', 3), selectedSeats: [seat('A', 2)]),
      isNull,
    );
    expect(
      validate
          .checkAddition(seat: seat('A', 5), selectedSeats: [seat('A', 2)])
          ?.type,
      SeatSelectionViolationType.notContiguous,
    );
    expect(
      validate
          .checkAddition(seat: seat('B', 2), selectedSeats: [seat('A', 2)])
          ?.type,
      SeatSelectionViolationType.differentRow,
    );
  });
}
