import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/dtos/showtime_dto.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/data/services/showtime_api_service.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/showtime.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/showtime_repository.dart';
import 'package:dio/dio.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';

class ShowtimeRepositoryImpl implements ShowtimeRepository {
  ShowtimeRepositoryImpl(this._apiService);

  final ShowtimeApiService _apiService;

  @override
  Future<ApiResult<List<Showtime>>> getByMovieAndDate({
    required String movieId,
    required DateTime date,
  }) async {
    try {
      final response = await _apiService.getByMovieAndDate(
        movieId: movieId,
        date: date,
      );

      final body = response.data;
      if (body is! List) {
        throw const FormatException('Expected a list of showtimes.');
      }

      final showtimes = body
          .map<Showtime>((item) {
            if (item is! Map) {
              throw const FormatException('Invalid showtime item.');
            }

            final json = Map<String, dynamic>.from(item);
            return ShowtimeDto.fromJson(json).toEntity();
          })
          .toList(growable: false);

      return ApiResult.success(showtimes);
    } on DioException catch (error) {
      final payload = error.response?.data;
      final message = payload is Map ? payload['message']?.toString() : null;

      return ApiResult.failure(
        Failure(
          message ?? 'Không thể tải danh sách suất chiếu.',
          statusCode: error.response?.statusCode,
        ),
      );
    } on FormatException catch (error) {
      return ApiResult.failure(
        Failure('Dữ liệu suất chiếu không hợp lệ: ${error.message}'),
      );
    } catch (error) {
      return ApiResult.failure(
        Failure('Không thể tải danh sách suất chiếu: $error'),
      );
    }
  }
}
