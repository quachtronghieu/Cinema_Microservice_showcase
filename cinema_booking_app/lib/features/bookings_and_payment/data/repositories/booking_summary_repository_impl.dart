import 'package:dio/dio.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/network/api_result.dart';
import '../../domain/entities/booking_summary.dart';
import '../../domain/repositories/booking_summary_repository.dart';
import '../dtos/customer_booking_dto.dart';
import '../services/booking_summary_api_service.dart';

class BookingSummaryRepositoryImpl implements BookingSummaryRepository {
  BookingSummaryRepositoryImpl(this._apiService);

  final BookingSummaryApiService _apiService;

  @override
  Future<ApiResult<BookingQuote>> preview(BookingSelection selection) async {
    try {
      final response = await _apiService.preview(
        CustomerBookingRequestDto(selection).toJson(),
      );
      return ApiResult.success(
        BookingQuoteDto(_asMap(response.data)).toEntity(),
      );
    } on DioException catch (error) {
      return ApiResult.failure(_failure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  @override
  Future<ApiResult<CreatedBooking>> create(
    BookingSelection selection, {
    required String idempotencyKey,
  }) async {
    try {
      final response = await _apiService.create(
        CustomerBookingRequestDto(selection).toJson(),
        idempotencyKey: idempotencyKey,
      );
      return ApiResult.success(
        CreatedBookingDto(_asMap(response.data)).toEntity(),
      );
    } on DioException catch (error) {
      return ApiResult.failure(_failure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  Map<String, dynamic> _asMap(dynamic data) => data is Map
      ? Map<String, dynamic>.from(data)
      : throw const FormatException('Phản hồi booking không hợp lệ.');

  Failure _failure(DioException error) {
    final data = error.response?.data;
    final body = data is Map ? Map<String, dynamic>.from(data) : null;
    return Failure(
      body?['message']?.toString() ??
          body?['error']?.toString() ??
          'Yêu cầu không thể hoàn tất. Vui lòng thử lại.',
      statusCode: error.response?.statusCode,
    );
  }
}
