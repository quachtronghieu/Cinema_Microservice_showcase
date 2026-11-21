import '../../domain/entities/cinema_models.dart';
import '../../domain/repositories/concession_repository.dart';
import '../mappers/concession_mapper.dart';
import '../services/concession_api_service.dart';

import 'package:dio/dio.dart';

import '../../../../core/errors/app_failure.dart';

class ConcessionApiRepository implements ConcessionRepository {
  ConcessionApiRepository({
    required this.service,
    required this.backendBaseUrl,
  });

  final ConcessionApiService service;
  final Uri backendBaseUrl;

  @override
  Future<List<CinemaConcession>> listConcessions() async {
    try {
      return await _loadAllPages();
    } on AppFailure {
      rethrow;
    } on DioException catch (error) {
      throw _mapDioError(error);
    } catch (error) {
      final type = error is FormatException || error is TypeError
          ? AppFailureType.invalidData
          : AppFailureType.unknown;

      throw _failure(type, error);
    }
  }

  Future<List<CinemaConcession>> _loadAllPages() async {
    final items = <CinemaConcession>[];
    final ids = <String>{};
    int? total;
    var page = 0;

    while (true) {
      final result = await service.fetchPage(page: page, pageSize: 20);

      if (result.page != page ||
          result.size != 20 ||
          (total != null && total != result.totalElements) ||
          result.items.length !=
              (result.totalElements - page * result.size).clamp(
                0,
                result.size,
              )) {
        throw const FormatException(
          'Thông tin phân trang catalog không hợp lệ.',
        );
      }
      total ??= result.totalElements;

      for (final dto in result.items) {
        if (!ids.add(dto.concessionId)) {
          throw const FormatException('Danh mục bị trùng món giữa các trang.');
        }
        items.add(mapConcessionDto(dto, backendBaseUrl: backendBaseUrl));
      }

      if (page + 1 >= result.totalPages) {
        return List<CinemaConcession>.unmodifiable(items);
      }

      page++;
    }
  }

  AppFailure _mapDioError(DioException error) {
    final status = error.response?.statusCode;
    var type = AppFailureType.unknown;

    if (status == 401) {
      type = AppFailureType.sessionExpired;
    } else if (status == 403) {
      type = AppFailureType.forbidden;
    } else if (status != null && status >= 500) {
      type = AppFailureType.server;
    } else if (error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.badCertificate) {
      type = AppFailureType.network;
    } else if (error.error is FormatException || error.error is TypeError) {
      type = AppFailureType.invalidData;
    }

    return _failure(type, error);
  }

  AppFailure _failure(AppFailureType type, Object cause) {
    final message = switch (type) {
      AppFailureType.sessionExpired => 'Bạn cần đăng nhập lại để xem danh mục.',
      AppFailureType.forbidden =>
        'Tài khoản không có quyền xem danh mục bắp/nước.',
      AppFailureType.network =>
        'Không kết nối được máy chủ. Kiểm tra mạng và thử lại.',
      AppFailureType.invalidData =>
        'Không đọc được dữ liệu danh mục. Vui lòng thử lại.',
      AppFailureType.server => 'Máy chủ đang gặp lỗi. Vui lòng thử lại sau.',
      AppFailureType.unknown => 'Không tải được danh mục. Vui lòng thử lại.',
    };

    return AppFailure(type: type, message: message, cause: cause);
  }
}
