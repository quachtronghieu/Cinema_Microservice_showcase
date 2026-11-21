import 'package:flutter/foundation.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_result.dart';
import '../../data/repositories/account_repository_impl.dart';
import '../../data/services/account_api_service.dart';
import '../../data/services/account_local_storage_service.dart';
import '../../domain/entities/customer_profile.dart';
import '../../domain/entities/user_session.dart';
import '../../domain/repositories/account_repository.dart';

class AccountViewModel extends ChangeNotifier {
  AccountViewModel([
    AccountRepository? repository,
    AccountLocalStorageService? localStorage,
  ]) : _repository =
           repository ??
           AccountRepositoryImpl(
             AccountApiService(dio: ApiClient.instance.dio),
           ),
       _localStorage = localStorage ?? AccountLocalStorageService.instance {
    ApiClient.sessionExpired.addListener(_handleSessionExpired);
  }

  final AccountRepository _repository;
  final AccountLocalStorageService _localStorage;
  bool _isLoading = false;
  String? _errorMessage;
  UserSession? _userSession;
  CustomerProfile? _profile;
  int _sessionGeneration = 0;
  bool _disposed = false;

  bool get busy => _isLoading;
  bool get loggedIn => _userSession?.isAuthenticated ?? false;
  String? get error => _errorMessage;
  UserSession? get userSession => _userSession;
  CustomerProfile? get profile => _profile;
  void _handleSessionExpired() {
    if (!ApiClient.sessionExpired.value) return;

    _sessionGeneration++;
    _userSession = null;
    _profile = null;
    _isLoading = false;
    _errorMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';

    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _sessionGeneration++;
    ApiClient.sessionExpired.removeListener(_handleSessionExpired);
    super.dispose();
  }

  Future<bool> login(String username, String password) async {
    final generation = ++_sessionGeneration;
    _userSession = null;
    _profile = null;
    _setLoading(clearError: true);
    final result = await _repository.login(username, password);
    if (!_isCurrentSession(generation)) return false;

    if (result case Success<UserSession>(:final data)) {
      final profile =
          await _localStorage.getProfile(data.username) ??
          _profileFromSession(data);
      if (!_isCurrentSession(generation)) return false;
      _userSession = data;
      _profile = profile;
      _setLoading(value: false);
      return true;
    }

    _errorMessage = (result as FailureResult<UserSession>).failure.message;
    _setLoading(value: false);
    return false;
  }

  Future<bool> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  }) async {
    _setLoading(clearError: true);
    final result = await _repository.register(
      username: username,
      password: password,
      confirmPassword: confirmPassword,
      email: email,
      phone: phone,
      dob: dob,
      sex: sex,
    );

    if (result case Success<String>()) {
      await _localStorage.saveProfile(
        CustomerProfile(
          username: username,
          email: email,
          phone: phone,
          dob: dob,
          sex: sex ? 'Nam' : 'Nữ',
        ),
      );
      _setLoading(value: false);
      return true;
    }

    _errorMessage = (result as FailureResult<String>).failure.message;
    _setLoading(value: false);
    return false;
  }

  Future<bool> forgotPassword(String email) async {
    _setLoading(clearError: true);
    final result = await _repository.forgotPassword(email);
    if (result case Success<String>()) {
      _setLoading(value: false);
      return true;
    }

    _errorMessage = (result as FailureResult<String>).failure.message;
    _setLoading(value: false);
    return false;
  }

  Future<bool> resetPassword({
    required String id,
    required int resetTimestamp,
    required String resetCode,
    required String newPassword,
    required String confirmPassword,
  }) async {
    _setLoading(clearError: true);
    final result = await _repository.resetPassword(
      id: id,
      resetTimestamp: resetTimestamp,
      resetCode: resetCode,
      newPassword: newPassword,
      confirmPassword: confirmPassword,
    );

    if (result case Success<String>()) {
      _setLoading(value: false);
      return true;
    }

    _errorMessage = (result as FailureResult<String>).failure.message;
    _setLoading(value: false);
    return false;
  }

  Future<bool> updateProfile({
    required String phone,
    required String dob,
    required String sex,
  }) async {
    final current = _profile;
    if (current == null) return false;
    final updated = current.copyWith(phone: phone, dob: dob, sex: sex);
    await _localStorage.updateProfile(updated);
    _profile = updated;
    notifyListeners();
    return true;
  }

  Future<void> logout() async {
    _sessionGeneration++;
    _isLoading = true;
    notifyListeners();
    await _repository.logout();
    await ApiClient.instance.cookieJar.deleteAll();
    _userSession = null;
    _profile = null;
    _isLoading = false;
    notifyListeners();
  }

  Future<void> checkSession() async {
    final generation = _sessionGeneration;
    final result = await _repository.getMe();
    if (!_isCurrentSession(generation)) return;
    if (result case Success<UserSession>(:final data)
        when data.isAuthenticated) {
      final profile =
          await _localStorage.getProfile(data.username) ??
          _profileFromSession(data);
      if (!_isCurrentSession(generation)) return;
      _userSession = data;
      _profile = profile;
    }
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  CustomerProfile _profileFromSession(UserSession session) =>
      CustomerProfile(userId: session.userId, username: session.username);

  bool _isCurrentSession(int generation) =>
      !_disposed && generation == _sessionGeneration;

  void _setLoading({bool value = true, bool clearError = false}) {
    if (_disposed) return;
    _isLoading = value;
    if (clearError) _errorMessage = null;
    notifyListeners();
  }
}
