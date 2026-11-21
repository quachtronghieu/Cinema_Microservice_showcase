import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/account/data/services/account_local_storage_service.dart';
import 'package:cinema_booking_app/features/account/domain/entities/customer_profile.dart';
import 'package:cinema_booking_app/features/account/domain/entities/user_session.dart';
import 'package:cinema_booking_app/features/account/domain/repositories/account_repository.dart';
import 'package:cinema_booking_app/features/account/presentation/screens/reset_password_screen.dart';
import 'package:cinema_booking_app/features/account/presentation/view_models/account_view_model.dart';
import 'package:provider/provider.dart';

class _FakeLocalStorage extends AccountLocalStorageService {
  _FakeLocalStorage() : super.testing();
  final Map<String, CustomerProfile> profiles = {};
  @override
  Future<void> saveProfile(CustomerProfile p) async => profiles[p.username] = p;
  @override
  Future<CustomerProfile?> getProfile(String u) async => profiles[u];
  @override
  Future<void> updateProfile(CustomerProfile p) async =>
      profiles[p.username] = p;
}

class _FakeAccountRepository implements AccountRepository {
  _FakeAccountRepository({this.resetPasswordResult});

  final ApiResult<String>? resetPasswordResult;

  @override
  Future<ApiResult<UserSession>> login(
    String username,
    String password,
  ) async => ApiResult.failure(const Failure('Not implemented'));

  @override
  Future<ApiResult<String>> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  }) async => ApiResult.failure(const Failure('Not implemented'));

  @override
  Future<ApiResult<UserSession>> getMe() async =>
      ApiResult.failure(const Failure('No session'));

  @override
  Future<ApiResult<void>> logout() async => ApiResult.success(null);

  @override
  Future<ApiResult<String>> forgotPassword(String email) async =>
      ApiResult.success('OK');

  @override
  Future<ApiResult<String>> resetPassword({
    required String id,
    required int resetTimestamp,
    required String resetCode,
    required String newPassword,
    required String confirmPassword,
  }) async =>
      resetPasswordResult ??
      ApiResult.success('Password changed successfully.');
}

void main() {
  testWidgets(
    'renders expired state when link timestamp is older than 10 minutes',
    (tester) async {
      final now = DateTime.now().millisecondsSinceEpoch;
      final expiredTimestamp = now - 15 * 60 * 1000; // 15 mins ago

      final vm = AccountViewModel(
        _FakeAccountRepository(),
        _FakeLocalStorage(),
      );

      await tester.pumpWidget(
        ChangeNotifierProvider<AccountViewModel>.value(
          value: vm,
          child: MaterialApp(
            home: ResetPasswordScreen(
              id: 'CUS_001',
              resetTimestamp: expiredTimestamp,
              resetCode: 'token_abc',
            ),
          ),
        ),
      );

      expect(find.text('Liên kết đã hết hạn'), findsOneWidget);
      expect(find.text('Quay lại Đăng nhập'), findsOneWidget);
    },
  );

  testWidgets('renders malformed state when id or token is missing', (
    tester,
  ) async {
    final vm = AccountViewModel(_FakeAccountRepository(), _FakeLocalStorage());

    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>.value(
        value: vm,
        child: const MaterialApp(
          home: ResetPasswordScreen(id: '', resetTimestamp: 0, resetCode: ''),
        ),
      ),
    );

    expect(find.text('Liên kết không hợp lệ'), findsOneWidget);
    expect(find.text('Quay lại Đăng nhập'), findsOneWidget);
  });

  testWidgets('renders active form and submits password when link is valid', (
    tester,
  ) async {
    final now = DateTime.now().millisecondsSinceEpoch;
    final validTimestamp = now - 60 * 1000; // 1 min ago

    final vm = AccountViewModel(_FakeAccountRepository(), _FakeLocalStorage());

    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>.value(
        value: vm,
        child: MaterialApp(
          home: ResetPasswordScreen(
            id: 'CUS_999',
            resetTimestamp: validTimestamp,
            resetCode: 'token_valid',
          ),
        ),
      ),
    );

    expect(find.text('Thiết lập mật khẩu mới'), findsOneWidget);
    expect(find.text('Đã nhận diện tài khoản ID: CUS_999'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Mật khẩu mới'), findsOneWidget);
    expect(
      find.widgetWithText(TextFormField, 'Xác nhận mật khẩu mới'),
      findsOneWidget,
    );

    // Enter matching passwords
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Mật khẩu mới'),
      'NewPass123!',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Xác nhận mật khẩu mới'),
      'NewPass123!',
    );

    await tester.tap(
      find.widgetWithText(FilledButton, 'Xác nhận đổi mật khẩu'),
    );
    await tester.pumpAndSettle();

    expect(find.text('Mật khẩu xác nhận không khớp.'), findsNothing);
  });

  testWidgets('displays error message when server rejects token', (
    tester,
  ) async {
    final now = DateTime.now().millisecondsSinceEpoch;
    final validTimestamp = now - 60 * 1000;

    final vm = AccountViewModel(
      _FakeAccountRepository(
        resetPasswordResult: ApiResult.failure(
          const Failure('Mã xác thực không còn hiệu lực.'),
        ),
      ),
      _FakeLocalStorage(),
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>.value(
        value: vm,
        child: MaterialApp(
          home: ResetPasswordScreen(
            id: 'CUS_999',
            resetTimestamp: validTimestamp,
            resetCode: 'token_invalid',
          ),
        ),
      ),
    );

    await tester.enterText(
      find.widgetWithText(TextFormField, 'Mật khẩu mới'),
      'NewPass123!',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Xác nhận mật khẩu mới'),
      'NewPass123!',
    );

    await tester.tap(
      find.widgetWithText(FilledButton, 'Xác nhận đổi mật khẩu'),
    );
    await tester.pumpAndSettle();

    expect(find.text('Mã xác thực không còn hiệu lực.'), findsOneWidget);
  });
}
