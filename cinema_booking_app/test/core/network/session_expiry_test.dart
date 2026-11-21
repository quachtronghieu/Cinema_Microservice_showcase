import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:cinema_booking_app/app/app.dart';
import 'package:cinema_booking_app/app/app_providers.dart';
import 'package:cinema_booking_app/app/app_router.dart';
import 'package:cinema_booking_app/core/network/api_client.dart';
import 'package:cinema_booking_app/features/account/data/repositories/account_repository_impl.dart';
import 'package:cinema_booking_app/features/account/data/services/account_api_service.dart';
import 'package:cinema_booking_app/features/account/data/services/account_local_storage_service.dart';
import 'package:cinema_booking_app/features/account/domain/entities/customer_profile.dart';
import 'package:cinema_booking_app/features/account/presentation/screens/login_registration_screen.dart';
import 'package:cinema_booking_app/features/account/presentation/view_models/account_view_model.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';

// HTTP responses are fixtures; no real credentials, JWTs or network are used.
class _FixtureAdapter implements HttpClientAdapter {
  final requests = <RequestOptions>[];
  FutureOr<ResponseBody> Function(RequestOptions)? respond;
  int catalogStatus = 200;
  int loginStatus = 200;
  String token = 'fixture-session-a';

  static ResponseBody jsonResponse(int status, Object body) =>
      ResponseBody.fromString(
        jsonEncode(body),
        status,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );

  static Map<String, Object> user() => {
    'userId': 'fixture-user',
    'username': 'fixture_customer',
    'role': 'ROLE_USER',
  };

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    if (respond != null) return await respond!(options);
    if (options.path == '/api/auth/login') {
      return jsonResponse(loginStatus, {
        ...user(),
        if (loginStatus == 200) 'accessToken': token,
      });
    }
    if (options.path == '/api/auth/me') {
      return jsonResponse(
        options.headers['Authorization'] == null ? 401 : 200,
        user(),
      );
    }
    if (options.path == '/api/v1/concessions') {
      return jsonResponse(catalogStatus, {
        'items': [
          {
            'concessionId': 'a6e35a32-150c-4045-af67-cf1ec4bca9d1',
            'type': 'PC',
            'name': 'Fixture Popcorn',
            'description': 'Fixture item.',
            'price': 50000,
            'img': '',
          },
        ],
        'page': 0,
        'size': 20,
        'totalElements': 1,
        'totalPages': 1,
      });
    }
    return jsonResponse(200, {});
  }

  @override
  void close({bool force = false}) {}
}

class _MemoryProfiles extends AccountLocalStorageService {
  _MemoryProfiles() : super.testing();

  Future<CustomerProfile?> Function()? readProfile;

  @override
  Future<CustomerProfile?> getProfile(String username) async =>
      readProfile == null ? null : await readProfile!();
}

Future<void> _expectStatus(Future<dynamic> request, int status) => expectLater(
  request,
  throwsA(
    isA<DioException>().having(
      (error) => error.response?.statusCode,
      'HTTP status',
      status,
    ),
  ),
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late ApiClient client;
  late _FixtureAdapter adapter;

  setUp(() async {
    ApiClient.reset();
    ApiClient.sessionExpired.value = false;
    adapter = _FixtureAdapter();
    final dio = Dio(BaseOptions(baseUrl: 'http://fixture.invalid'))
      ..httpClientAdapter = adapter;
    client = await ApiClient.getInstance(dio: dio, cookieJar: CookieJar());
  });

  tearDown(() {
    client.dio.close(force: true);
    ApiClient.reset();
    ApiClient.sessionExpired.value = false;
  });

  AccountViewModel account([_MemoryProfiles? profiles]) {
    final model = AccountViewModel(
      AccountRepositoryImpl(AccountApiService(dio: client.dio)),
      profiles ?? _MemoryProfiles(),
    );
    addTearDown(model.dispose);
    return model;
  }

  test(
    '401 clears current bearer, account and profile without retry',
    () async {
      final model = account();
      expect(await model.login('fixture_customer', 'fixture-password'), isTrue);
      expect(model.profile, isNotNull);
      adapter.catalogStatus = 401;

      await _expectStatus(client.dio.get('/api/v1/concessions'), 401);

      expect(client.dio.options.headers['Authorization'], isNull);
      expect(ApiClient.sessionExpired.value, isTrue);
      expect(model.loggedIn, isFalse);
      expect(model.profile, isNull);
      expect(model.error, contains('hết hạn'));
      expect(
        adapter.requests.where((r) => r.path == '/api/v1/concessions'),
        hasLength(1),
      );
    },
  );

  for (final status in [403, 500]) {
    test('$status preserves bearer and authenticated account', () async {
      final model = account();
      await model.login('fixture_customer', 'fixture-password');
      adapter.catalogStatus = status;

      await _expectStatus(client.dio.get('/api/v1/concessions'), status);

      expect(
        client.dio.options.headers['Authorization'],
        'Bearer ${adapter.token}',
      );
      expect(ApiClient.sessionExpired.value, isFalse);
      expect(model.loggedIn, isTrue);
    });
  }

  test('invalid login 401 does not trigger session-expiry redirect', () async {
    final model = account();
    adapter.loginStatus = 401;
    expect(await model.login('fixture_customer', 'fixture-password'), isFalse);
    expect(ApiClient.sessionExpired.value, isFalse);
    expect(model.loggedIn, isFalse);
  });

  test(
    'late 401 from previous bearer preserves a newly issued bearer',
    () async {
      final service = AccountApiService(dio: client.dio);
      service.setAccessToken('fixture-old');
      final started = Completer<void>();
      final response = Completer<ResponseBody>();
      adapter.respond = (_) {
        started.complete();
        return response.future;
      };
      final failure = _expectStatus(client.dio.get('/api/v1/concessions'), 401);
      await started.future;

      service.setAccessToken('fixture-new');
      response.complete(_FixtureAdapter.jsonResponse(401, {}));
      await failure;

      expect(client.dio.options.headers['Authorization'], 'Bearer fixture-new');
      expect(ApiClient.sessionExpired.value, isFalse);
    },
  );

  test(
    'concurrent current-token 401 responses expire the session once',
    () async {
      AccountApiService(dio: client.dio).setAccessToken(adapter.token);
      adapter.catalogStatus = 401;
      var events = 0;
      void listener() => events++;
      ApiClient.sessionExpired.addListener(listener);
      addTearDown(() => ApiClient.sessionExpired.removeListener(listener));

      await Future.wait([
        _expectStatus(client.dio.get('/api/v1/concessions'), 401),
        _expectStatus(client.dio.get('/api/v1/concessions'), 401),
      ]);

      expect(events, 1);
      expect(ApiClient.sessionExpired.value, isTrue);
    },
  );

  test('successful re-login resets expiry and uses the new bearer', () async {
    final model = account();
    await model.login('fixture_customer', 'fixture-password');
    adapter.catalogStatus = 401;
    await _expectStatus(client.dio.get('/api/v1/concessions'), 401);

    adapter.token = 'fixture-session-b';
    adapter.catalogStatus = 200;
    expect(await model.login('fixture_customer', 'fixture-password'), isTrue);
    await client.dio.get('/api/v1/concessions');

    expect(ApiClient.sessionExpired.value, isFalse);
    expect(model.loggedIn, isTrue);
    expect(
      adapter.requests.last.headers['Authorization'],
      'Bearer fixture-session-b',
    );
  });

  test('late me response cannot restore an expired account', () async {
    final model = account();
    await model.login('fixture_customer', 'fixture-password');
    final started = Completer<void>();
    final response = Completer<ResponseBody>();
    adapter.respond = (options) {
      if (options.path == '/api/auth/me') {
        started.complete();
        return response.future;
      }
      return _FixtureAdapter.jsonResponse(401, {});
    };
    final check = model.checkSession();
    await started.future;
    await _expectStatus(client.dio.get('/api/v1/concessions'), 401);
    response.complete(
      _FixtureAdapter.jsonResponse(200, _FixtureAdapter.user()),
    );
    await check;

    expect(model.loggedIn, isFalse);
    expect(model.profile, isNull);
  });

  test('late profile read cannot finish login after session expiry', () async {
    final started = Completer<void>();
    final profile = Completer<CustomerProfile?>();
    final storage = _MemoryProfiles()
      ..readProfile = () {
        started.complete();
        return profile.future;
      };
    final model = account(storage);
    final login = model.login('fixture_customer', 'fixture-password');
    await started.future;
    adapter.catalogStatus = 401;
    await _expectStatus(client.dio.get('/api/v1/concessions'), 401);
    profile.complete(null);

    expect(await login, isFalse);
    expect(model.loggedIn, isFalse);
    expect(model.profile, isNull);
    expect(model.error, contains('hết hạn'));
  });

  testWidgets(
    'production router and providers clear cart and request re-login',
    (tester) async {
      final documents = (await tester.runAsync(
        () => Directory.systemTemp.createTemp('cinema-expiry-'),
      ))!;
      const pathChannel = MethodChannel('plugins.flutter.io/path_provider');
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(pathChannel, (_) async => documents.path);
      addTearDown(() async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(pathChannel, null);
        await documents.delete(recursive: true);
      });

      await tester.pumpWidget(const AppProviders(child: CinemaApp()));
      await tester.pumpAndSettle();
      final context = tester.element(find.byType(LoginRegistrationScreen));
      final model = context.read<AccountViewModel>();
      expect(
        await tester.runAsync(
          () => model.login('fixture_customer', 'fixture-password'),
        ),
        isTrue,
      );
      appRouter.go('/concessions');
      await tester.pumpAndSettle();
      expect(find.text('Fixture Popcorn'), findsOneWidget);
      final catalog = tester
          .element(find.byType(ConcessionCatalogScreen))
          .read<ConcessionsViewModel>();
      catalog.setQuantity(catalog.catalog.single.id, 1);
      await tester.pumpAndSettle();
      expect(catalog.count, 1);

      adapter.catalogStatus = 401;
      await tester.tap(find.byTooltip('Tải lại danh sách'));
      await tester.pumpAndSettle();

      expect(find.byType(LoginRegistrationScreen), findsOneWidget);
      expect(find.byType(ConcessionCatalogScreen), findsNothing);
      expect(model.loggedIn, isFalse);
      expect(catalog.catalog, isEmpty);
      expect(catalog.cart, isEmpty);

      adapter.token = 'fixture-session-b';
      adapter.catalogStatus = 200;
      expect(
        await tester.runAsync(
          () => model.login('fixture_customer', 'fixture-password'),
        ),
        isTrue,
      );
      appRouter.go('/concessions');
      await tester.pumpAndSettle();
      expect(find.text('Fixture Popcorn'), findsOneWidget);
      expect(catalog.cart, isEmpty);
      await tester.pumpWidget(const SizedBox.shrink());
      appRouter.go('/login');
    },
  );
}
