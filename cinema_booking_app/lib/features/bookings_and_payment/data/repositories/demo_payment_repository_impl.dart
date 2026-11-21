import 'package:dio/dio.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/network/api_result.dart';
import '../../domain/entities/demo_payment.dart';
import '../../domain/repositories/demo_payment_repository.dart';
import '../dtos/demo_payment_dto.dart';
import '../services/demo_payment_api_service.dart';

class DemoPaymentRepositoryImpl implements DemoPaymentRepository {
  DemoPaymentRepositoryImpl(this._apiService);

  final DemoPaymentApiService _apiService;

  @override
  Future<ApiResult<DemoPaymentResult>> process({
    required String bookingId,
    required DemoPaymentOutcome outcome,
  }) async {
    try {
      final response = await _apiService.process(
        bookingId: bookingId,
        request: DemoPaymentRequestDto(outcome).toJson(),
      );
      final data = response.data;
      if (data is! Map) {
        throw const FormatException('Phản hồi thanh toán không hợp lệ.');
      }
      return ApiResult.success(
        DemoPaymentResultDto(Map<String, dynamic>.from(data)).toEntity(),
      );
    } on DioException catch (error) {
      return ApiResult.failure(_failure(error));
    } on FormatException catch (error) {
      return ApiResult.failure(Failure(error.message));
    } catch (_) {
      return ApiResult.failure(
        const Failure('Không thể xử lý thanh toán demo. Vui lòng thử lại.'),
      );
    }
  }

  Failure _failure(DioException error) {
    final data = error.response?.data;
    final body = data is Map ? Map<String, dynamic>.from(data) : null;
    return Failure(
      body?['message']?.toString() ??
          'Yêu cầu thanh toán không thể hoàn tất. Vui lòng thử lại.',
      statusCode: error.response?.statusCode,
    );
  }
}
