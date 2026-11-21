import 'dart:async';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:cinema_booking_app/core/errors/app_failure.dart';
import 'package:cinema_booking_app/core/config/api_config.dart';
import 'package:cinema_booking_app/core/network/api_client.dart';
import 'package:cinema_booking_app/features/customer/data/repositories/concession_api_repository.dart';
import 'package:cinema_booking_app/features/customer/data/services/concession_api_service.dart';
import 'package:cinema_booking_app/features/customer/domain/entities/cinema_models.dart';
import 'package:cinema_booking_app/features/customer/domain/repositories/concession_repository.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';

import '../../support/catalog_fixture.dart';

class _ScreenRepository implements ConcessionRepository {
  late Future<List<CinemaConcession>> Function() respond;
  @override
  Future<List<CinemaConcession>> listConcessions() => respond();
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late ApiClient client;
  late CatalogFixture fixture;
  late CatalogAdapter adapter;
  late ConcessionApiRepository repository;

  setUp(() async {
    ApiClient.reset();
    ApiClient.sessionExpired.value = false;
    fixture = CatalogFixture();
    adapter = CatalogAdapter(fixture);
    final dio = Dio(
      BaseOptions(
        baseUrl: 'http://catalog.fixture',
        headers: {'Authorization': 'Bearer ${CatalogFixture.token}'},
      ),
    )..httpClientAdapter = adapter;
    client = await ApiClient.getInstance(dio: dio, cookieJar: CookieJar());
    repository = ConcessionApiRepository(
      service: ConcessionApiService(client),
      backendBaseUrl: Uri.parse(dio.options.baseUrl),
    );
  });

  tearDown(() {
    client.dio.close(force: true);
    ApiClient.reset();
    ApiClient.sessionExpired.value = false;
  });

  ConcessionsViewModel viewModel() {
    final model = ConcessionsViewModel(repository);
    addTearDown(model.dispose);
    return model;
  }

  test('default size 20 loads all 21 items without duplicates', () async {
    final items = await repository.listConcessions();
    expect(items, hasLength(21));
    expect(items.map((item) => item.id).toSet(), hasLength(21));
    expect(adapter.requests.map((r) => r.queryParameters['page']), [0, 1]);
    expect(
      adapter.requests.every((r) => r.queryParameters['size'] == 20),
      isTrue,
    );
    expect(
      adapter.requests.every((r) => r.path == '/api/v1/concessions'),
      isTrue,
    );
  });

  test(
    'UUID category filtering includes drinks from the second page',
    () async {
      final model = viewModel();
      await model.load();
      model.setFilter(ConcessionFilter.popcorn);
      expect(model.filteredCatalog, hasLength(20));
      model.setFilter(ConcessionFilter.drink);
      expect(
        model.filteredCatalog.single.id,
        fixture.items.last['concessionId'],
      );
      model.setFilter(ConcessionFilter.all);
      expect(model.filteredCatalog, hasLength(21));
    },
  );

  test('empty catalog stops after the first page', () async {
    fixture.items.clear();
    expect(await repository.listConcessions(), isEmpty);
    expect(adapter.requests, hasLength(1));
  });

  test('exactly 20 items do not request a nonexistent extra page', () async {
    fixture.items.removeLast();
    expect(await repository.listConcessions(), hasLength(20));
    expect(adapter.requests, hasLength(1));
  });

  test(
    'a later-page failure never publishes a partial successful catalog',
    () async {
      adapter.respond = (options) async => options.queryParameters['page'] == 1
          ? CatalogReply(503, {})
          : fixture.route(
              options.method,
              options.uri,
              options.headers['Authorization'] as String?,
            );
      final model = viewModel();
      await model.load();
      expect(model.catalog, isEmpty);
      expect(model.error, contains('Máy chủ'));
      expect(model.busy, isFalse);
    },
  );

  for (final scenario in [
    'wrong page',
    'zero pages with items',
    'duplicate ID',
    'short page',
    'changed total',
    'wrong size',
    'fractional page',
  ]) {
    test('rejects malformed pagination: $scenario', () async {
      adapter.respond = (options) async {
        final page = options.queryParameters['page'] as int;
        final selected = fixture.items.skip(page * 20).take(20).toList();
        final body = catalogPage(selected, page: page, total: 21);
        if (scenario == 'wrong page') body['page'] = 99;
        if (scenario == 'zero pages with items') body['totalPages'] = 0;
        if (scenario == 'duplicate ID' && page == 1) {
          body['items'] = [fixture.items.first];
        }
        if (scenario == 'short page' && page == 0) {
          body['items'] = [fixture.items.first];
        }
        if (scenario == 'changed total' && page == 1) {
          body['totalElements'] = 22;
        }
        if (scenario == 'wrong size') {
          body['size'] = 21;
          body['totalPages'] = 1;
        }
        if (scenario == 'fractional page') body['page'] = 0.5;
        return CatalogReply(200, body);
      };
      await expectLater(
        repository.listConcessions(),
        throwsA(
          isA<AppFailure>().having(
            (error) => error.type,
            'failure type',
            AppFailureType.invalidData,
          ),
        ),
      );
    });
  }

  test('normal defaults target Gateway for Windows and Android', () {
    addTearDown(() {
      debugDefaultTargetPlatformOverride = null;
      ApiConfig.reset();
    });
    debugDefaultTargetPlatformOverride = TargetPlatform.windows;
    ApiConfig.reset();
    expect(ApiConfig.baseUrl, 'http://localhost:8080');
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    ApiConfig.reset();
    expect(ApiConfig.baseUrl, 'http://10.0.2.2:8080');
  });

  test(
    'inactive item is absent after refresh and its customer detail is 404',
    () async {
      final model = viewModel();
      await model.load();
      final id = fixture.items.last['concessionId'];
      model.setQuantity(id as String, 1);
      fixture.items.last['active'] = false;
      await model.load();
      expect(model.catalog.any((item) => item.id == id), isFalse);
      expect(model.cart.containsKey(id), isFalse);
      await expectLater(
        client.dio.get('/api/v1/concessions/$id'),
        throwsA(
          isA<DioException>().having(
            (error) => error.response?.statusCode,
            'status',
            404,
          ),
        ),
      );
    },
  );

  test(
    'refresh from session reset cannot restore an old in-flight catalog',
    () async {
      final model = viewModel();
      final started = Completer<void>();
      final response = Completer<CatalogReply>();
      adapter.respond = (_) {
        started.complete();
        return response.future;
      };
      final load = model.load();
      await started.future;
      model.reset(clearCatalog: true);
      response.complete(CatalogReply(200, catalogPage([catalogItem(1)])));
      await load;
      expect(model.catalog, isEmpty);
    },
  );

  test(
    'integral decimal JSON price is accepted, fractional price is rejected',
    () async {
      fixture.items.removeRange(1, fixture.items.length);
      fixture.items.single['price'] = 25000.00;
      expect((await repository.listConcessions()).single.price, 25000);
      fixture.items.single['price'] = 25000.50;
      await expectLater(
        repository.listConcessions(),
        throwsA(
          isA<AppFailure>().having(
            (error) => error.type,
            'failure type',
            AppFailureType.invalidData,
          ),
        ),
      );
    },
  );

  Future<void> show(WidgetTester tester, ConcessionsViewModel model) async {
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: model,
        child: const MaterialApp(home: ConcessionCatalogScreen()),
      ),
    );
  }

  List<CinemaConcession> screenItems() => [
    const CinemaConcession(
      id: '00000000-0000-4000-8000-000000000001',
      type: 'PC',
      name: 'Fixture Popcorn 1',
      description: 'Fixture item.',
      price: 25000,
      image: '',
    ),
    const CinemaConcession(
      id: '00000000-0000-4000-8000-000000000021',
      type: 'DR',
      name: 'Fixture Cola',
      description: 'Fixture item.',
      price: 25000,
      image: '',
    ),
  ];

  ConcessionsViewModel screenModel(_ScreenRepository source) {
    final model = ConcessionsViewModel(source);
    addTearDown(model.dispose);
    return model;
  }

  testWidgets(
    'loading, UUID filters and inactive empty state render correctly',
    (tester) async {
      final response = Completer<List<CinemaConcession>>();
      final source = _ScreenRepository()..respond = () => response.future;
      final model = screenModel(source);
      await show(tester, model);
      final load = model.load();
      await tester.pump();
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      response.complete(screenItems());
      await tester.pumpAndSettle();
      await load;
      await tester.tap(find.widgetWithText(ChoiceChip, 'Nước'));
      await tester.pumpAndSettle();
      expect(find.text('Fixture Cola'), findsOneWidget);
      expect(find.text('Fixture Popcorn 1'), findsNothing);
      source.respond = () async => [screenItems().first];
      await tester.tap(find.byTooltip('Tải lại danh sách'));
      await tester.pumpAndSettle();
      expect(find.text('Không có món thuộc loại đã chọn.'), findsOneWidget);
    },
  );

  testWidgets('initial error can retry and recover', (tester) async {
    final source = _ScreenRepository()
      ..respond = () async => throw const AppFailure(
        type: AppFailureType.server,
        message: 'Máy chủ đang gặp lỗi. Vui lòng thử lại sau.',
      );
    final model = screenModel(source);
    await model.load();
    await show(tester, model);
    await tester.pumpAndSettle();
    expect(
      find.text('Máy chủ đang gặp lỗi. Vui lòng thử lại sau.'),
      findsOneWidget,
    );
    source.respond = () async => screenItems();
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();
    expect(model.catalog, hasLength(2));
    expect(model.error, isNull);
  });

  testWidgets(
    'failed refresh reports the error even with an existing catalog',
    (tester) async {
      final source = _ScreenRepository()..respond = () async => screenItems();
      final model = screenModel(source);
      await model.load();
      await show(tester, model);
      await tester.pumpAndSettle();
      source.respond = () async => throw const AppFailure(
        type: AppFailureType.server,
        message: 'Máy chủ đang gặp lỗi. Vui lòng thử lại sau.',
      );
      await tester.tap(find.byTooltip('Tải lại danh sách'));
      await tester.pumpAndSettle();
      expect(
        find.text('Máy chủ đang gặp lỗi. Vui lòng thử lại sau.'),
        findsOneWidget,
      );
      source.respond = () async => screenItems();
      await tester.tap(find.text('Retry'));
      await tester.pumpAndSettle();
      expect(model.error, isNull);
    },
  );
}
