import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/account/data/services/account_local_storage_service.dart';
import 'package:cinema_booking_app/features/account/domain/entities/customer_profile.dart';
import 'package:cinema_booking_app/features/account/domain/entities/user_session.dart';
import 'package:cinema_booking_app/features/account/domain/repositories/account_repository.dart';
import 'package:cinema_booking_app/features/account/presentation/screens/account_screen.dart';
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
  _FakeAccountRepository({this.initialSession});

  final UserSession? initialSession;

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
  Future<ApiResult<UserSession>> getMe() async {
    if (initialSession != null) return ApiResult.success(initialSession!);
    return ApiResult.failure(const Failure('No session'));
  }

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
  }) async => ApiResult.success('OK');
}

void main() {
  testWidgets('renders login prompt when not logged in', (tester) async {
    final vm = AccountViewModel(_FakeAccountRepository(), _FakeLocalStorage());

    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>.value(
        value: vm,
        child: const MaterialApp(home: AccountScreen()),
      ),
    );

    expect(
      find.text('Vui lòng đăng nhập để xem thông tin tài khoản'),
      findsOneWidget,
    );
  });

  testWidgets('renders profile info and opens edit sheet when logged in', (
    tester,
  ) async {
    const session = UserSession(
      userId: 'CS000001',
      username: 'john_doe',
      role: 'Customer',
      isAuthenticated: true,
    );
    final vm = AccountViewModel(
      _FakeAccountRepository(initialSession: session),
      _FakeLocalStorage(),
    );
    await vm.checkSession();

    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ChangeNotifierProvider<AccountViewModel>.value(
        value: vm,
        child: const MaterialApp(home: AccountScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('@john_doe'), findsOneWidget);
    expect(find.text('Thông tin cá nhân'), findsOneWidget);
    expect(find.text('Chỉnh sửa'), findsOneWidget);
    expect(find.text('Đăng xuất'), findsOneWidget);

    // Open edit profile sheet
    await tester.tap(find.text('Chỉnh sửa'));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextFormField, 'Số điện thoại'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Ngày sinh'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Lưu thay đổi'), findsOneWidget);
  });
}
