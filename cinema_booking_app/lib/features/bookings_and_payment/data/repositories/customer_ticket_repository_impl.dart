import 'package:dio/dio.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/network/api_result.dart';
import '../../domain/entities/customer_ticket.dart';
import '../../domain/repositories/customer_ticket_repository.dart';
import '../dtos/customer_ticket_dto.dart';
import '../services/customer_ticket_api_service.dart';

class CustomerTicketRepositoryImpl implements CustomerTicketRepository {
  CustomerTicketRepositoryImpl(this._apiService);

  final CustomerTicketApiService _apiService;

  @override
  Future<ApiResult<List<CustomerTicket>>> findAll() async {
    try {
      final data = (await _apiService.findAll()).data;
      if (data is! List) {
        throw const FormatException('Danh sách vé không hợp lệ.');
      }
      return ApiResult.success(
        data
            .map(
              (item) =>
                  CustomerTicketDto(Map<String, dynamic>.from(item as Map))
                      .toEntity(),
            )
            .toList(growable: false),
      );
    } on DioException catch (error) {
      return ApiResult.failure(_failure(error));
    } on FormatException catch (error) {
      return ApiResult.failure(Failure(error.message));
    } catch (_) {
      return ApiResult.failure(
        const Failure('Không thể tải danh sách vé. Vui lòng thử lại.'),
      );
    }
  }

  @override
  Future<ApiResult<CustomerTicket>> findById(String ticketId) async {
    try {
      final data = (await _apiService.findById(ticketId)).data;
      if (data is! Map) {
        throw const FormatException('Thông tin vé không hợp lệ.');
      }
      return ApiResult.success(
        CustomerTicketDto(Map<String, dynamic>.from(data)).toEntity(),
      );
    } on DioException catch (error) {
      return ApiResult.failure(_failure(error));
    } on FormatException catch (error) {
      return ApiResult.failure(Failure(error.message));
    } catch (_) {
      return ApiResult.failure(
        const Failure('Không thể tải vé. Vui lòng thử lại.'),
      );
    }
  }

  Failure _failure(DioException error) {
    final data = error.response?.data;
    final body = data is Map ? Map<String, dynamic>.from(data) : null;
    return Failure(
      body?['message']?.toString() ??
          'Yêu cầu không thể hoàn tất. Vui lòng thử lại.',
      statusCode: error.response?.statusCode,
    );
  }
}
