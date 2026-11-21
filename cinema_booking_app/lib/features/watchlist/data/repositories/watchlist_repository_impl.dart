import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/errors/failures.dart';
import '../../../../core/network/api_result.dart';
import '../../../movies/data/dtos/movie_dto.dart';
import '../../../movies/data/mappers/movie_mapper.dart';
import '../../../movies/domain/entities/movie.dart';
import '../../domain/repositories/watchlist_repository.dart';
import '../services/watchlist_api_service.dart';

class WatchlistRepositoryImpl implements WatchlistRepository {
  WatchlistRepositoryImpl(this._apiService);

  final WatchlistApiService _apiService;

  @override
  Future<ApiResult<List<Movie>>> getWatchlistMovies() async {
    try {
      final response = await _apiService.getWatchlist();
      final data = response.data;
      if (data is! List) {
        throw const FormatException('Expected a JSON array of movies.');
      }
      final movies = data
          .whereType<Map<String, dynamic>>()
          .map((json) => MovieDto.fromJson(json).toEntity())
          .toList(growable: false);
      return ApiResult.success(movies);
    } on DioException catch (error) {
      return ApiResult.failure(
        _dioFailure(error, fallback: 'Không tải được watchlist.'),
      );
    } on FormatException {
      return ApiResult.failure(
        const Failure('Dữ liệu watchlist từ máy chủ không hợp lệ.'),
      );
    } catch (_) {
      return ApiResult.failure(
        const Failure('Đã xảy ra lỗi khi xử lý watchlist.'),
      );
    }
  }

  @override
  Future<ApiResult<void>> add(String movieId) => _write(
    () => _apiService.add(movieId),
    fallback: 'Không thể thêm phim vào watchlist.',
  );

  @override
  Future<ApiResult<void>> remove(String movieId) => _write(
    () => _apiService.remove(movieId),
    fallback: 'Không thể bỏ phim khỏi watchlist.',
  );

  Future<ApiResult<void>> _write(
    Future<Response<dynamic>> Function() request, {
    required String fallback,
  }) async {
    try {
      await request();
      return ApiResult.success(null);
    } on DioException catch (error) {
      return ApiResult.failure(_dioFailure(error, fallback: fallback));
    } catch (_) {
      return ApiResult.failure(Failure(fallback));
    }
  }

  Failure _dioFailure(DioException error, {required String fallback}) {
    final statusCode = error.response?.statusCode;
    if (statusCode == null) {
      final baseUrl = error.requestOptions.baseUrl.isNotEmpty
          ? error.requestOptions.baseUrl
          : ApiConfig.baseUrl;
      return Failure(
        'Không thể kết nối máy chủ tại $baseUrl. '
        'Kiểm tra API_BASE_URL và kết nối mạng rồi thử lại.',
      );
    }
    // 302 về trang đăng nhập của Spring Security cũng nghĩa là hết phiên.
    if (statusCode == 302 || statusCode == 401 || statusCode == 403) {
      return Failure(
        'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: statusCode,
      );
    }
    final body = error.response?.data;
    // Chỉ coi là "không có phim" khi controller trả code NOT_FOUND; 404 của
    // Spring (chưa có route) không mang code này.
    if (statusCode == 404 &&
        body is Map<String, dynamic> &&
        body['code'] == 'NOT_FOUND') {
      return const Failure('Không tìm thấy phim.', statusCode: 404);
    }
    final message = body is Map<String, dynamic>
        ? body['message']?.toString()
        : null;
    return Failure(
      message?.isNotEmpty == true ? message! : '$fallback ($statusCode)',
      statusCode: statusCode,
    );
  }
}
