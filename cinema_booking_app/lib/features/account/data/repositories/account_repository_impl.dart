import 'package:dio/dio.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/network/api_result.dart';
import '../../domain/entities/user_session.dart';
import '../../domain/repositories/account_repository.dart';
import '../dtos/login_request_dto.dart';
import '../dtos/register_request_dto.dart';
import '../dtos/reset_password_request_dto.dart';
import '../dtos/user_info_dto.dart';
import '../services/account_api_service.dart';

class AccountRepositoryImpl implements AccountRepository {
  AccountRepositoryImpl(this._apiService);

  final AccountApiService _apiService;

  @override
Future<ApiResult<UserSession>> login(
  String username,
  String password,
) async {
  _apiService.clearAccessToken();

  try {
    final response = await _apiService.login(
      LoginRequestDto(username: username, password: password),
    );

    final body = _asMap(response.data);
    final accessToken = body?['accessToken'];

    if (accessToken is! String || accessToken.trim().isEmpty) {
      throw const FormatException(
        'Phản hồi đăng nhập không có accessToken hợp lệ.',
      );
    }

    final user = UserInfoDto.fromJson(_userPayload(response.data));
    final session = _session(user);

    _apiService.setAccessToken(accessToken);

    return ApiResult.success(session);
  } on DioException catch (error) {
    return ApiResult.failure(_dioFailure(error));
  } catch (_) {
    return ApiResult.failure(
      const Failure('Không đọc được kết quả đăng nhập.'),
    );
  }
}

  @override
  Future<ApiResult<String>> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  }) async {
    try {
      final response = await _apiService.register(
        RegisterRequestDto(
          username: username,
          password: password,
          confirmPassword: confirmPassword,
          email: email,
          phone: phone,
          dob: dob,
          sex: sex,
        ),
      );
      final body = _asMap(response.data);
      final message = body?['message'] ?? body?['data'] ?? response.data;
      return ApiResult.success(
        message?.toString() ?? 'Registration successful',
      );
    } on DioException catch (error) {
      return ApiResult.failure(_dioFailure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  @override
  Future<ApiResult<String>> forgotPassword(String email) async {
    try {
      final response = await _apiService.forgotPassword(email);
      final body = _asMap(response.data);
      final message = body?['message'] ?? body?['data'] ?? response.data;
      return ApiResult.success(message?.toString() ?? 'Yêu cầu đã được gửi.');
    } on DioException catch (error) {
      return ApiResult.failure(_dioFailure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  @override
  Future<ApiResult<String>> resetPassword({
    required String id,
    required int resetTimestamp,
    required String resetCode,
    required String newPassword,
    required String confirmPassword,
  }) async {
    try {
      final response = await _apiService.resetPassword(
        ResetPasswordRequestDto(
          id: id,
          resetTimestamp: resetTimestamp,
          resetCode: resetCode,
          newPassword: newPassword,
          confirmPassword: confirmPassword,
        ),
      );
      final body = _asMap(response.data);
      final message = body?['message'] ?? body?['data'] ?? response.data;
      return ApiResult.success(
        message?.toString() ?? 'Đặt lại mật khẩu thành công.',
      );
    } on DioException catch (error) {
      return ApiResult.failure(_dioFailure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  @override
  Future<ApiResult<UserSession>> getMe() async {
    try {
      final response = await _apiService.getMe();
      final user = UserInfoDto.fromJson(_userPayload(response.data));
      return ApiResult.success(_session(user));
    } on DioException catch (error) {
      return ApiResult.failure(_dioFailure(error));
    } catch (error) {
      return ApiResult.failure(Failure(error.toString()));
    }
  }

  @override
Future<ApiResult<void>> logout() async {
  try {
    await _apiService.logout();
    return ApiResult.success(null);
  } on DioException catch (error) {
    return ApiResult.failure(_dioFailure(error));
  } catch (_) {
    return ApiResult.failure(
      const Failure('Không thực hiện được yêu cầu đăng xuất.'),
    );
  } finally {
    _apiService.clearAccessToken();
  }
}

  UserSession _session(UserInfoDto user) => UserSession(
    userId: user.userId,
    username: user.username,
    role: user.role,
    isAuthenticated: true,
  );

  Map<String, dynamic> _userPayload(dynamic data) {
    final initial = _asMap(data);
    if (initial == null) {
      throw const FormatException('Expected a JSON object in auth response.');
    }
    var result = initial;
    for (final key in ['data', 'user']) {
      final nested = _asMap(result[key]);
      if (nested != null) result = nested;
    }
    return result;
  }

  Map<String, dynamic>? _asMap(dynamic value) =>
      value is Map<String, dynamic> ? value : null;

  Failure _dioFailure(DioException error) {
    final data = _asMap(error.response?.data);
    Map<String, dynamic>? errors = _asMap(data?['errors']);
    String? backendMessage;

    final rawError = data?['error'];
    if (rawError is Map<String, dynamic>) {
      backendMessage = rawError['message']?.toString();
      final details = _asMap(rawError['details']);
      if (details != null && details.isNotEmpty) {
        errors = {...?errors, ...details};
      }
    } else if (rawError is String) {
      backendMessage = rawError;
    }

    if (backendMessage == null && errors != null && errors.isNotEmpty) {
      backendMessage = errors.values.map((v) => v.toString()).join('\n');
    }

    final message =
        backendMessage ??
        data?['message']?.toString() ??
        (error.response?.data is String &&
                (error.response!.data as String).trim().isNotEmpty
            ? (error.response!.data as String).trim()
            : null) ??
        (error.response?.statusCode == 400
            ? 'Dữ liệu không hợp lệ hoặc đã tồn tại trên hệ thống.'
            : 'Yêu cầu thất bại (${error.response?.statusCode ?? 'Lỗi kết nối'})');

    return Failure(
      message,
      statusCode: error.response?.statusCode,
      errors: errors,
    );
  }
}
