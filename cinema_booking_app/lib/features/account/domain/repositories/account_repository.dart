import '../../../../core/network/api_result.dart';
import '../entities/user_session.dart';

abstract interface class AccountRepository {
  Future<ApiResult<UserSession>> login(String username, String password);

  Future<ApiResult<String>> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  });

  Future<ApiResult<String>> forgotPassword(String email);

  Future<ApiResult<String>> resetPassword({
    required String id,
    required int resetTimestamp,
    required String resetCode,
    required String newPassword,
    required String confirmPassword,
  });

  Future<ApiResult<UserSession>> getMe();

  Future<ApiResult<void>> logout();
}
