import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/dtos/seat_hold_dto.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/services/seat_hold_api_service.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/seat_hold_repository.dart';
import 'package:dio/dio.dart';

class SeatHoldRepositoryImpl implements SeatHoldRepository {
  SeatHoldRepositoryImpl(this._apiService);

  final SeatHoldApiService _apiService;

  @override
  Future<ApiResult<SeatHold>> holdSeats({
    required String showtimeId,
    required List<String> seatIds,
  }) async {
    try {
      final response = await _apiService.holdSeats(
        showtimeId: showtimeId,
        seatIds: seatIds,
      );
      final body = response.data;
      if (body is! Map) {
        throw const FormatException('Expected a seat hold object.');
      }
      return ApiResult.success(
        SeatHoldDto.fromJson(Map<String, dynamic>.from(body))
            .toEntity(receivedAt: DateTime.now()),
      );
    } on DioException catch (error) {
      return ApiResult.failure(
        _failureFrom(error, fallback: 'Không thể giữ ghế. Vui lòng thử lại.'),
      );
    } on FormatException catch (error) {
      return ApiResult.failure(
        Failure('Dữ liệu giữ ghế không hợp lệ: ${error.message}'),
      );
    } on TypeError {
      return ApiResult.failure(const Failure('Dữ liệu giữ ghế không hợp lệ.'));
    }
  }

  @override
  Future<ApiResult<void>> releaseHold(String holdId) async {
    try {
      await _apiService.releaseHold(holdId);
      return ApiResult.success(null);
    } on DioException catch (error) {
      // 404: hold đã hết hạn hoặc đã được nhả, ghế không còn bị mình giữ.
      if (error.response?.statusCode == 404) return ApiResult.success(null);
      return ApiResult.failure(
        _failureFrom(
          error,
          fallback: 'Không thể bỏ giữ ghế. Vui lòng thử lại.',
        ),
      );
    }
  }

  /// Giữ nguyên `code`/`seatIds` của backend trong [Failure.errors] để UI
  /// hiển thị ghế bị xung đột.
  Failure _failureFrom(DioException error, {required String fallback}) {
    final statusCode = error.response?.statusCode;
    if (statusCode == 401 || statusCode == 403) {
      return Failure(
        'Vui lòng đăng nhập bằng tài khoản khách hàng để giữ ghế.',
        statusCode: statusCode,
      );
    }
    if (statusCode == null) {
      return const Failure('Không thể kết nối máy chủ. Vui lòng thử lại.');
    }
    final payload = error.response?.data;
    final errors = payload is Map ? Map<String, dynamic>.from(payload) : null;
    return Failure(
      errors?['message']?.toString() ?? fallback,
      statusCode: statusCode,
      errors: errors,
    );
  }
}
