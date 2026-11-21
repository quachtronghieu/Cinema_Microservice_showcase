import 'package:flutter/services.dart';

import 'package:flutter_test/flutter_test.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_client.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/core/ui/validators.dart';
import 'package:cinema_booking_app/features/account/domain/entities/user_session.dart';
import 'package:cinema_booking_app/features/account/domain/repositories/account_repository.dart';
import 'package:cinema_booking_app/features/account/presentation/view_models/account_view_model.dart';
import 'package:cinema_booking_app/features/account/data/services/account_local_storage_service.dart';
import 'package:cinema_booking_app/features/account/domain/entities/customer_profile.dart';

class FakeAccountRepository implements AccountRepository {
  ApiResult<UserSession> loginResponse = ApiResult.failure(
    const Failure('Login failed'),
  );
  ApiResult<String> registerResponse = ApiResult.failure(
    const Failure('Registration failed'),
  );
  ApiResult<UserSession> getMeResponse = ApiResult.failure(
    const Failure('Session unavailable'),
  );
  ApiResult<void> logoutResponse = ApiResult.success(null);
  ApiResult<String> resetPasswordResponse = ApiResult.success(
    'Password reset successfully',
  );

  @override
  Future<ApiResult<UserSession>> login(
    String username,
    String password,
  ) async => loginResponse;

  @override
  Future<ApiResult<String>> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  }) async => registerResponse;

  @override
  Future<ApiResult<UserSession>> getMe() async => getMeResponse;

  @override
  Future<ApiResult<void>> logout() async => logoutResponse;

  @override
  Future<ApiResult<String>> forgotPassword(String email) async =>
      ApiResult.success('Request sent');

  @override
  Future<ApiResult<String>> resetPassword({
    required String id,
    required int resetTimestamp,
    required String resetCode,
    required String newPassword,
    required String confirmPassword,
  }) async => resetPasswordResponse;
}

class FakeAccountLocalStorageService extends AccountLocalStorageService {
  FakeAccountLocalStorageService() : super.testing();

  final Map<String, CustomerProfile> profiles = {};

  @override
  Future<void> saveProfile(CustomerProfile profile) async {
    profiles[profile.username] = profile;
  }

  @override
  Future<CustomerProfile?> getProfile(String username) async =>
      profiles[username];

  @override
  Future<void> updateProfile(CustomerProfile profile) async {
    profiles[profile.username] = profile;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AccountViewModel', () {
    test('login success populates the authenticated session', () async {
      const session = UserSession(
        userId: 'CS000001',
        username: 'CinemaUser',
        role: 'customer',
        isAuthenticated: true,
      );
      final repository = FakeAccountRepository()
        ..loginResponse = ApiResult.success(session);
      final viewModel = AccountViewModel(
        repository,
        FakeAccountLocalStorageService(),
      );

      final result = await viewModel.login('CinemaUser', 'Password1!');

      expect(result, isTrue);
      expect(viewModel.loggedIn, isTrue);
      expect(viewModel.userSession, same(session));
      expect(viewModel.profile?.username, 'CinemaUser');
      expect(viewModel.error, isNull);
    });

    test(
      'login failure keeps logged out and exposes the failure message',
      () async {
        final repository = FakeAccountRepository()
          ..loginResponse = ApiResult.failure(
            const Failure('Invalid credentials'),
          );
        final viewModel = AccountViewModel(repository);

        final result = await viewModel.login('CinemaUser', 'wrong');

        expect(result, isFalse);
        expect(viewModel.loggedIn, isFalse);
        expect(viewModel.error, contains('Invalid credentials'));
      },
    );

    test('register success returns true and leaves error null', () async {
      final repository = FakeAccountRepository()
        ..registerResponse = ApiResult.success('Registered');
      final viewModel = AccountViewModel(
        repository,
        FakeAccountLocalStorageService(),
      );

      final result = await viewModel.register(
        username: 'CinemaUser',
        password: 'Password1!',
        confirmPassword: 'Password1!',
        email: 'cinema@example.com',
        phone: '0123456789',
        dob: '2000-01-01',
        sex: true,
      );

      expect(result, isTrue);
      expect(viewModel.error, isNull);
    });

    test(
      'register failure returns false and exposes the failure message',
      () async {
        final repository = FakeAccountRepository()
          ..registerResponse = ApiResult.failure(
            const Failure('Email is taken'),
          );
        final viewModel = AccountViewModel(repository);

        final result = await viewModel.register(
          username: 'CinemaUser',
          password: 'Password1!',
          confirmPassword: 'Password1!',
          email: 'cinema@example.com',
          phone: '0123456789',
          dob: '2000-01-01',
          sex: true,
        );

        expect(result, isFalse);
        expect(viewModel.error, contains('Email is taken'));
      },
    );

    test('logout clears session and profile', () async {
      final repository = FakeAccountRepository()
        ..loginResponse = ApiResult.success(
          const UserSession(
            userId: 'CS000001',
            username: 'CinemaUser',
            role: 'customer',
            isAuthenticated: true,
          ),
        );
      final viewModel = AccountViewModel(repository);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('plugins.flutter.io/path_provider'),
            (methodCall) async => '/tmp/mobile-test-documents',
          );
      await ApiClient.getInstance();

      await viewModel.login('CinemaUser', 'Password1!');
      expect(viewModel.profile, isNotNull);

      await viewModel.logout();

      expect(viewModel.userSession, isNull);
      expect(viewModel.profile, isNull);
      expect(viewModel.loggedIn, isFalse);
    });

    test('resetPassword success returns true', () async {
      final repository = FakeAccountRepository();
      final viewModel = AccountViewModel(repository);

      final success = await viewModel.resetPassword(
        id: 'CS000001',
        resetTimestamp: 123456789,
        resetCode: 'test-code',
        newPassword: 'NewPassword1!',
        confirmPassword: 'NewPassword1!',
      );

      expect(success, isTrue);
      expect(viewModel.error, isNull);
      expect(viewModel.busy, isFalse);
    });

    test('resetPassword failure sets error message', () async {
      final repository = FakeAccountRepository()
        ..resetPasswordResponse = ApiResult.failure(
          const Failure('Mã đặt lại không hợp lệ.'),
        );
      final viewModel = AccountViewModel(repository);

      final success = await viewModel.resetPassword(
        id: 'CS000001',
        resetTimestamp: 123456789,
        resetCode: 'bad-code',
        newPassword: 'NewPassword1!',
        confirmPassword: 'NewPassword1!',
      );

      expect(success, isFalse);
      expect(viewModel.error, 'Mã đặt lại không hợp lệ.');
      expect(viewModel.busy, isFalse);
    });
  });

  group('validators', () {
    test('valid and invalid emails', () {
      expect(validateEmail('person@example.com'), isNull);
      expect(validateEmail('not-an-email'), isNotNull);
      expect(validateEmail(''), isNotNull);
    });

    test('valid and invalid phone numbers', () {
      expect(validatePhone('0123456789'), isNull);
      expect(validatePhone('12345678'), isNotNull);
      expect(validatePhone('12345abcde'), isNotNull);
    });

    test('valid and invalid passwords', () {
      expect(validatePassword('Strong1!'), isNull);
      expect(validatePassword('weak'), isNotNull);
      expect(validatePassword(''), isNotNull);
    });
  });
}
