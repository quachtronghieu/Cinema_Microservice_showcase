import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:provider/provider.dart';

import 'package:cinema_booking_app/app/app.dart';
import 'package:cinema_booking_app/app/app_providers.dart';
import 'package:cinema_booking_app/app/app_router.dart';
import 'package:cinema_booking_app/core/config/api_config.dart';
import 'package:cinema_booking_app/core/network/api_client.dart';
import 'package:cinema_booking_app/features/account/presentation/screens/login_registration_screen.dart';
import 'package:cinema_booking_app/features/account/presentation/view_models/account_view_model.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_cart_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';

import '../test/support/catalog_fixture.dart';

Future<void> waitFor(WidgetTester tester, bool Function() ready) async {
  final deadline = DateTime.now().add(const Duration(seconds: 30));
  while (!ready()) {
    if (DateTime.now().isAfter(deadline)) {
      fail('Timed out waiting for catalog UI.');
    }
    await tester.pump(const Duration(milliseconds: 100));
  }
  await tester.pump();
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Android reaches the real development Gateway health endpoint', (
    tester,
  ) async {
    final http = HttpClient();
    try {
      final request = await http
          .getUrl(Uri.parse('http://10.0.2.2:8080/actuator/health'))
          .timeout(const Duration(seconds: 10));
      final response = await request.close().timeout(
        const Duration(seconds: 10),
      );
      expect(response.statusCode, 200);
      final body = jsonDecode(await utf8.decoder.bind(response).join()) as Map;
      expect(body['status'], 'UP');
    } finally {
      http.close(force: true);
    }
  }, skip: !const bool.fromEnvironment('CHECK_LIVE_GATEWAY'));

  testWidgets(
    'Android catalog and cart: paging, add, navigation, inactive and retry',
    (tester) async {
      // The device runs the production app with a local HTTP API fixture, not a
      // real Identity account or live Catalog database. No production config changes.
      final fixture = CatalogFixture(images: true);
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      final subscription = server.listen((request) async {
        await request.drain<void>();
        final reply = fixture.route(
          request.method,
          request.uri,
          request.headers.value(HttpHeaders.authorizationHeader),
        );
        request.response.statusCode = reply.status;
        request.response.headers.contentType = reply.image
            ? ContentType('image', 'png')
            : ContentType.json;
        if (reply.image) {
          request.response.add(reply.body as Uint8List);
        } else {
          request.response.write(jsonEncode(reply.body));
        }
        await request.response.close();
      });
      final baseUrl = 'http://127.0.0.1:${server.port}';
      ApiClient.reset();
      ApiClient.sessionExpired.value = false;
      ApiConfig.configure(baseUrl: baseUrl);
      final client = await ApiClient.getInstance(
        dio: Dio(BaseOptions(baseUrl: baseUrl)),
        cookieJar: CookieJar(),
      );

      try {
        await tester.pumpWidget(const AppProviders(child: CinemaApp()));
        await tester.pump();
        final account = tester
            .element(find.byType(LoginRegistrationScreen))
            .read<AccountViewModel>();
        expect(
          await tester.runAsync(
            () => account.login('fixture_customer', 'fixture-password'),
          ),
          isTrue,
        );
        appRouter.go('/concessions');
        await waitFor(
          tester,
          () => find.byType(ConcessionCatalogScreen).evaluate().isNotEmpty,
        );
        final catalog = tester
            .element(find.byType(ConcessionCatalogScreen))
            .read<ConcessionsViewModel>();
        await waitFor(
          tester,
          () => !catalog.busy && catalog.catalog.length == 21,
        );

        final pages = fixture.requests
            .where((uri) => uri.path == '/api/v1/concessions')
            .toList();
        expect(pages.map((uri) => uri.queryParameters['page']), ['0', '1']);
        expect(
          pages.every((uri) => uri.queryParameters['size'] == '20'),
          isTrue,
        );
        await tester.tap(find.widgetWithText(ChoiceChip, 'Bắp'));
        await tester.pump();
        expect(catalog.filteredCatalog, hasLength(20));
        await tester.tap(find.widgetWithText(ChoiceChip, 'Nước'));
        await tester.pump();
        expect(find.text('Fixture Cola'), findsOneWidget);
        expect(catalog.filteredCatalog, hasLength(1));
        await waitFor(tester, () => fixture.imagesServed > 0);

        await tester.tap(find.byTooltip('Thêm Fixture Cola'));
        await tester.tap(find.byTooltip('Thêm Fixture Cola'));
        await tester.pump();
        expect(catalog.count, 2);
        await tester.tap(find.text('Xem giỏ (2)'));
        await tester.pumpAndSettle();
        expect(find.byType(ConcessionCartScreen), findsOneWidget);
        expect(catalog.cart.values.single, 2);
        expect(
          tester
              .widget<ElevatedButton>(
                find.widgetWithText(ElevatedButton, 'Tiếp tục'),
              )
              .onPressed,
          isNull,
        );
        await tester.tap(find.text('Thêm món'));
        await tester.pumpAndSettle();
        expect(find.text('Xem giỏ (2)'), findsOneWidget);
        fixture.items.last['active'] = false;
        await tester.tap(find.byTooltip('Tải lại danh sách'));
        await waitFor(
          tester,
          () => !catalog.busy && catalog.catalog.length == 20,
        );
        expect(find.text('Fixture Cola'), findsNothing);
        expect(find.text('Không có món thuộc loại đã chọn.'), findsOneWidget);
        expect(catalog.cart, isEmpty);
        expect(find.textContaining('Đã bỏ món'), findsOneWidget);
        await expectLater(
          client.dio.get(
            '/api/v1/concessions/${fixture.items.last['concessionId']}',
          ),
          throwsA(
            isA<DioException>().having(
              (error) => error.response?.statusCode,
              'status',
              404,
            ),
          ),
        );

        fixture.failCatalog = true;
        await tester.tap(find.byTooltip('Tải lại danh sách'));
        await waitFor(tester, () => !catalog.busy && catalog.error != null);
        expect(
          find.text('Máy chủ đang gặp lỗi. Vui lòng thử lại sau.'),
          findsOneWidget,
        );
        fixture.failCatalog = false;
        await tester.tap(find.text('Retry'));
        await waitFor(tester, () => !catalog.busy && catalog.error == null);
        expect(catalog.catalog, hasLength(20));
      } finally {
        await tester.pumpWidget(const SizedBox.shrink());
        appRouter.go('/login');
        client.dio.close(force: true);
        ApiClient.reset();
        ApiClient.sessionExpired.value = false;
        ApiConfig.reset();
        await subscription.cancel();
        await server.close(force: true);
      }
    },
  );
}
