import '../../../../core/network/api_client.dart';
import '../dtos/concession_page_dto.dart';

class ConcessionApiService {
  ConcessionApiService(this.client);

  final ApiClient client;

  Future<ConcessionPageDto> fetchPage({
    int page = 0,
    int pageSize = 20,
  }) async {
    if (page < 0) {
      throw ArgumentError.value(page, 'page', 'Trang bắt đầu từ 0.');
    }

    if (pageSize < 1 || pageSize > 100) {
      throw ArgumentError.value(
        pageSize,
        'pageSize',
        'Số sản phẩm mỗi trang phải từ 1 đến 100.',
      );
    }

    final response = await client.dio.get<Map<String, dynamic>>(
      '/api/v1/concessions',
      queryParameters: {
        'page': page,
        'size': pageSize,
      },
    );

    final json = response.data;
    if (json == null) {
      throw const FormatException('API danh mục trả về dữ liệu trống.');
    }

    return ConcessionPageDto.fromJson(json);
  }
}