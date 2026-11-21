import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../dtos/login_request_dto.dart';
import '../dtos/register_request_dto.dart';
import '../dtos/reset_password_request_dto.dart';

class AccountApiService {
  AccountApiService({Dio? dio}) : _dio = dio ?? ApiClient.instance.dio;

  final Dio _dio;

  Future<Response<dynamic>> login(LoginRequestDto request) =>
      _dio.post(ApiConfig.login, data: request.toJson());

  Future<Response<dynamic>> register(RegisterRequestDto request) =>
      _dio.post(ApiConfig.register, data: request.toJson());

  Future<Response<dynamic>> getMe() => _dio.get(ApiConfig.me);

  Future<Response<dynamic>> forgotPassword(String email) =>
      _dio.post(ApiConfig.forgotPassword, data: {'email': email});

  Future<Response<dynamic>> resetPassword(ResetPasswordRequestDto request) =>
      _dio.post(ApiConfig.resetPassword, data: request.toJson());

  Future<Response<dynamic>> logout() => _dio.post(ApiConfig.logout);
  void setAccessToken(String token) {
    _dio.options.headers['Authorization'] = 'Bearer ${token.trim()}';
    ApiClient.sessionExpired.value = false;
  }

void clearAccessToken() {
  _dio.options.headers.remove('Authorization');
}
}
