import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/dtos/seat_hold_dto.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/repositories/seat_map_repository_impl.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/services/seat_map_api_service.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> layout({String showtimeId = 'SW1'}) => {
  'showtimeId': showtimeId,
  'movieId': 'MV1',
  'movieTitle': 'DEMO',
  'roomId': '1',
  'templateId': 'TM000003',
  'showDate': '2026-11-06',
  'startTime': '10:00:00',
  'seats': [
    {
      'templateSeatId': 'TS1',
      'rowLabel': 'A',
      'seatNumber': 1,
      'seatType': 'STANDARD',
    },
    {
      'templateSeatId': 'TS2',
      'rowLabel': 'A',
      'seatNumber': 2,
      'seatType': 'VIP',
    },
    {
      'templateSeatId': 'TS3',
      'rowLabel': 'A',
      'seatNumber': 3,
      'seatType': 'VIP',
    },
    {
      'templateSeatId': 'TS4',
      'rowLabel': 'A',
      'seatNumber': 4,
      'seatType': 'VIP',
    },
    {
      'templateSeatId': 'TS5',
      'rowLabel': 'A',
      'seatNumber': 5,
      'seatType': 'VIP',
    },
  ],
};

Map<String, dynamic> availability() => {
  'showtimeId': 'SW1',
  'salesStatus': 'OPEN',
  'serverTime': '2026-11-05T15:00:00.000Z',
  'seats': [
    {
      'showtimeSeatId': 'HS1',
      'templateSeatId': 'TS1',
      'rowLabel': 'A',
      'seatNumber': 1,
      'seatType': 'STANDARD',
      'status': 'AVAILABLE',
      'unitPrice': 80000.00,
    },
    {
      'showtimeSeatId': 'HS2',
      'templateSeatId': 'TS2',
      'rowLabel': 'A',
      'seatNumber': 2,
      'seatType': 'VIP',
      'status': 'HELD',
      'unitPrice': 100000.00,
    },
    {
      'showtimeSeatId': 'HS3',
      'templateSeatId': 'TS3',
      'rowLabel': 'A',
      'seatNumber': 3,
      'seatType': 'VIP',
      'status': 'SOLD',
      'unitPrice': 100000.00,
    },
    {
      'showtimeSeatId': 'HS4',
      'templateSeatId': 'TS4',
      'rowLabel': 'A',
      'seatNumber': 4,
      'seatType': 'VIP',
      'status': 'STRANGE',
      'unitPrice': 100000.00,
    },
  ],
};

/// Trả response theo path để chạy service/repository thật, không gọi mạng.
SeatMapRepositoryImpl repository(
  Object? Function(String path) respond, {
  Set<String> failPaths = const {},
  int failStatus = 503,
  Object? failBody,
}) {
  final dio = Dio(BaseOptions(baseUrl: 'http://test'));
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        final response = Response<dynamic>(
          requestOptions: options,
          statusCode: 200,
          data: respond(options.path),
        );
        if (failPaths.contains(options.path)) {
          handler.reject(
            DioException(
              requestOptions: options,
              response: Response<dynamic>(
                requestOptions: options,
                statusCode: failStatus,
                data: failBody,
              ),
            ),
          );
        } else {
          handler.resolve(response);
        }
      },
    ),
  );
  return SeatMapRepositoryImpl(SeatMapApiService(dio));
}

const layoutPath = '/api/v1/showtimes/SW1/seats';
const availabilityPath = '/api/v1/showtimes/SW1/seat-availability';

Object? ok(String path) => path == layoutPath ? layout() : availability();

void main() {
  test('joins Cinema layout and Booking inventory by templateSeatId', () async {
    final result = await repository(ok).getSeatMap('SW1');
    final map = (result as Success<SeatMap>).data;

    expect(map.templateId, 'TM000003');
    expect(map.roomId, '1');
    expect(map.seats, hasLength(5));
    expect(map.seats[0].showtimeSeatId, 'HS1');
    expect(map.seats[0].templateSeatId, 'TS1');
    expect(map.seats[0].code, 'A1');
    expect(map.seats[0].price, 80000);
    expect(map.seats[1].type, SeatType.vip);
  });

  test('maps inventory statuses and only AVAILABLE is selectable', () async {
    final result = await repository(ok).getSeatMap('SW1');
    final seats = (result as Success<SeatMap>).data.seats;

    expect(seats[0].status, SeatStatus.available);
    expect(seats[0].isSelectable, isTrue);
    expect(seats[1].status, SeatStatus.pending);
    expect(seats[1].isSelectable, isFalse);
    expect(seats[2].status, SeatStatus.unavailable);
    expect(seats[2].isSelectable, isFalse);
    expect(seats[3].status, SeatStatus.unknown);
    expect(seats[3].isSelectable, isFalse);
  });

  test('a physical seat without inventory is never free', () async {
    final result = await repository(ok).getSeatMap('SW1');
    final seat = (result as Success<SeatMap>).data.seats[4];

    expect(seat.templateSeatId, 'TS5');
    expect(seat.showtimeSeatId, isEmpty);
    expect(seat.status, SeatStatus.unknown);
    expect(seat.isSelectable, isFalse);
  });

  test('Booking down gives a failure, not a map of free seats', () async {
    final result = await repository(
      ok,
      failPaths: {availabilityPath},
      failBody: {'code': 'SERVICE_UNAVAILABLE', 'message': 'Tạm thời lỗi'},
    ).getSeatMap('SW1');

    final failure = (result as FailureResult<SeatMap>).failure;
    expect(failure.message, 'Tạm thời lỗi');
    expect(failure.statusCode, 503);
  });

  test('Cinema down gives a failure with a default message', () async {
    final result = await repository(
      ok,
      failPaths: {layoutPath},
    ).getSeatMap('SW1');

    expect(
      (result as FailureResult<SeatMap>).failure.message,
      'Không thể tải sơ đồ ghế. Vui lòng thử lại.',
    );
  });

  test('responses of different showtimes are rejected', () async {
    final result = await repository(
      (path) =>
          path == layoutPath ? layout(showtimeId: 'OTHER') : availability(),
    ).getSeatMap('SW1');

    expect(result, isA<FailureResult<SeatMap>>());
  });

  test('a list body is reported as invalid data', () async {
    final result = await repository((path) => <Object>[]).getSeatMap('SW1');
    expect(
      (result as FailureResult<SeatMap>).failure.message,
      contains('không hợp lệ'),
    );
  });

  test(
    'deadline follows the server duration when the device clock is wrong',
    () {
      final hold = SeatHoldDto.fromJson({
        'holdId': 'H1',
        'showtimeId': 'SW1',
        'seatIds': ['HS1'],
        'expiresAt': '2026-11-05T15:05:00.000Z',
        'serverTime': '2026-11-05T15:00:00.000Z',
      }).toEntity(receivedAt: DateTime.utc(2000, 1, 1, 8));

      expect(hold.deadline, DateTime.utc(2000, 1, 1, 8, 5));
      expect(
        hold.remainingAt(DateTime.utc(2000, 1, 1, 8, 1)),
        const Duration(minutes: 4),
      );
      expect(hold.isExpiredAt(DateTime.utc(2000, 1, 1, 8, 5)), isTrue);
    },
  );
}
