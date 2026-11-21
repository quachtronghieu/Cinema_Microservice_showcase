import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/account/domain/entities/user_session.dart';
import 'package:cinema_booking_app/features/account/domain/repositories/account_repository.dart';
import 'package:cinema_booking_app/features/account/presentation/screens/login_registration_screen.dart';
import 'package:cinema_booking_app/features/account/presentation/view_models/account_view_model.dart';
import 'package:provider/provider.dart';

class _FakeAccountRepository implements AccountRepository {
  @override
  Future<ApiResult<UserSession>> login(
    String username,
    String password,
  ) async => ApiResult.failure(const Failure('Invalid credentials'));

  @override
  Future<ApiResult<String>> register({
    required String username,
    required String password,
    required String confirmPassword,
    required String email,
    required String phone,
    required String dob,
    required bool sex,
  }) async => ApiResult.failure(const Failure('Registration failed'));

  @override
  Future<ApiResult<UserSession>> getMe() async =>
      ApiResult.failure(const Failure('No session'));

  @override
  Future<ApiResult<void>> logout() async => ApiResult.success(null);

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
  }) async => ApiResult.success('Password reset successfully');
}

void main() {
  Future<void> pumpLoginRegistrationScreen(WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>(
        create: (_) => AccountViewModel(_FakeAccountRepository()),
        child: const MaterialApp(home: LoginRegistrationScreen()),
      ),
    );
  }

  testWidgets('renders wordmark, tabs, username and password fields', (
    tester,
  ) async {
    await pumpLoginRegistrationScreen(tester);

    expect(
      find.byWidgetPredicate((widget) {
        return widget is RichText &&
            widget.text.toPlainText().contains('CINEMA');
      }),
      findsOneWidget,
    );

    expect(find.text('Đăng nhập'), findsNWidgets(2));
    expect(find.text('Đăng ký'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Mật khẩu'), findsOneWidget);
  });

  testWidgets('registration tab reveals account detail fields', (tester) async {
    await pumpLoginRegistrationScreen(tester);
    await tester.tap(find.text('Đăng ký').first);
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextFormField, 'Email'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Số điện thoại'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Ngày sinh'), findsOneWidget);
    expect(find.text('Giới tính'), findsOneWidget);
  });

  testWidgets('submitting empty registration fields shows validation errors', (
    tester,
  ) async {
    await pumpLoginRegistrationScreen(tester);
    await tester.tap(find.text('Đăng ký').first);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.widgetWithText(FilledButton, 'Đăng ký'));
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng ký'));
    await tester.pumpAndSettle();

    expect(find.text('Username is required.'), findsOneWidget);
    expect(find.text('Password is required.'), findsOneWidget);
    expect(find.text('Mật khẩu xác nhận không khớp.'), findsNothing);
    expect(find.text('Email is required.'), findsOneWidget);
    expect(find.text('Phone number is required.'), findsOneWidget);
    expect(find.text('Ngày sinh là bắt buộc.'), findsOneWidget);
  });

  testWidgets('forgot password bottom sheet opens and switches steps', (
    tester,
  ) async {
    await pumpLoginRegistrationScreen(tester);

    // Tap 'Quên mật khẩu?'
    await tester.tap(find.text('Quên mật khẩu?'));
    await tester.pumpAndSettle();

    // Verify Step 1 is rendered inside modal bottom sheet
    expect(find.text('Quên mật khẩu'), findsOneWidget);
    expect(find.text('Bước 1: Nhận mã xác thực qua email'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Địa chỉ Email'), findsOneWidget);
    expect(
      find.widgetWithText(FilledButton, 'Gửi mã xác nhận'),
      findsOneWidget,
    );

    // Switch to Step 2
    await tester.tap(find.text('Đã có mã xác nhận? Đổi mật khẩu ngay'));
    await tester.pumpAndSettle();

    // Verify Step 2 is rendered
    expect(find.text('Đặt lại mật khẩu'), findsOneWidget);
    expect(find.text('Bước 2: Xác nhận mã và mật khẩu mới'), findsOneWidget);
    expect(
      find.widgetWithText(TextFormField, 'Liên kết hoặc Token từ email'),
      findsOneWidget,
    );
    expect(find.widgetWithText(TextFormField, 'Mật khẩu mới'), findsOneWidget);
    expect(
      find.widgetWithText(TextFormField, 'Xác nhận mật khẩu mới'),
      findsOneWidget,
    );
    expect(
      find.widgetWithText(FilledButton, 'Xác nhận đổi mật khẩu'),
      findsOneWidget,
    );

    // Enter a full reset password link and test auto-detection chip
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Liên kết hoặc Token từ email'),
      'http://localhost:5173/reset-password/CUS_001/1700000000/token123',
    );
    await tester.pumpAndSettle();

    expect(find.text('Đã nhận diện: ID CUS_001'), findsOneWidget);
  });
}
