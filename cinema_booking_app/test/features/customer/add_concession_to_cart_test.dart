import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'package:cinema_booking_app/features/customer/domain/entities/cinema_models.dart';
import 'package:cinema_booking_app/features/customer/domain/repositories/concession_repository.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_cart_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/entities/booking_summary.dart';

const popcorn = CinemaConcession(
  id: 'popcorn-id',
  type: 'PC',
  name: 'Popcorn',
  description: '',
  price: 50000,
  image: '',
);
const cola = CinemaConcession(
  id: 'cola-id',
  type: 'DR',
  name: 'Cola',
  description: '',
  price: 25000,
  image: '',
);

class CartRepository implements ConcessionRepository {
  Future<List<CinemaConcession>> Function() respond = () async => [
    popcorn,
    cola,
  ];
  @override
  Future<List<CinemaConcession>> listConcessions() => respond();
}

SeatHold hold(String id, {bool expired = false}) {
  final now = DateTime.now();
  final deadline = now.add(Duration(minutes: expired ? -1 : 5));
  return SeatHold(
    holdId: id,
    showtimeId: 'showtime',
    seatIds: ['seat'],
    expiresAt: deadline,
    serverTime: now,
    deadline: deadline,
  );
}

Future<void> showCatalog(
  WidgetTester tester,
  ConcessionsViewModel vm, {
  SeatHold? currentHold,
}) async {
  await tester.pumpWidget(
    ChangeNotifierProvider.value(
      value: vm,
      child: MaterialApp(home: ConcessionCatalogScreen(hold: currentHold)),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late CartRepository source;
  late ConcessionsViewModel vm;
  setUp(() async {
    source = CartRepository();
    vm = ConcessionsViewModel(source);
    await vm.load();
  });
  tearDown(() => vm.dispose());

  testWidgets('two taps before a rebuild add two units', (tester) async {
    await showCatalog(tester, vm);
    await tester.tap(find.byTooltip('Thêm Popcorn'));
    await tester.tap(find.byTooltip('Thêm Popcorn'));
    await tester.pumpAndSettle();
    expect(vm.cart, {popcorn.id: 2});
    expect(find.text('Xem giỏ (2)'), findsOneWidget);
  });

  test('add merges IDs and computes total units and temporary subtotal', () {
    vm.addItem(popcorn.id);
    vm.addItem(popcorn.id);
    vm.addItem(cola.id);
    expect(vm.cart, {popcorn.id: 2, cola.id: 1});
    expect(vm.count, 3);
    expect(vm.foodSubtotal, 125000);
    expect(() => vm.cart[popcorn.id] = 9, throwsUnsupportedError);
  });

  test(
    'unknown IDs do not enter the cart and commands cap each item at ten',
    () {
      vm.addItem('missing');
      expect(vm.cart, isEmpty);
      expect(vm.error, isNotNull);
      for (var i = 0; i < 15; i++) {
        vm.addItem(popcorn.id);
      }
      vm.addItem(cola.id);
      expect(vm.cart, {popcorn.id: 10, cola.id: 1});
      expect(vm.count, 11);
    },
  );

  test('temporary cart follows first hold; another hold clears it', () {
    vm.addItem(popcorn.id);
    vm.syncHold(null);
    vm.syncHold('A');
    vm.syncHold('A');
    expect(vm.count, 1);
    vm.syncHold('B');
    expect(vm.cart, isEmpty);
    expect(vm.cartNotice, contains('lượt giữ ghế trước'));
    vm.addItem(cola.id);
    vm.syncHold('B');
    expect(vm.cart, {cola.id: 1});
    vm.syncHold(null);
    expect(vm.cart, isEmpty);
  });

  test(
    'refresh removes only missing selections and names them in a notice',
    () async {
      vm.addItem(popcorn.id);
      vm.addItem(cola.id);
      source.respond = () async => [cola];
      await vm.load();
      expect(vm.cart, {cola.id: 1});
      expect(vm.foodSubtotal, 25000);
      expect(vm.cartNotice, contains('Popcorn'));
      expect(vm.cartNotice, isNot(contains('Cola')));
      vm.dismissCartNotice();
      expect(vm.cartNotice, isNull);
    },
  );

  test('failed refresh preserves selections and reports an error', () async {
    vm.addItem(popcorn.id);
    source.respond = () async => throw StateError('unavailable');
    await vm.load();
    expect(vm.cart, {popcorn.id: 1});
    expect(vm.foodSubtotal, 50000);
    expect(vm.error, isNotNull);
  });

  test('a successful refresh updates the displayed temporary price', () async {
    vm.addItem(popcorn.id);
    source.respond = () async => [
      const CinemaConcession(
        id: 'popcorn-id',
        type: 'PC',
        name: 'Popcorn',
        description: '',
        price: 55000,
        image: '',
      ),
    ];
    await vm.load();
    expect(vm.cart, {popcorn.id: 1});
    expect(vm.foodSubtotal, 55000);
  });

  test('account change and logout reset selections and hold context', () async {
    vm.syncSession('user-A');
    await Future<void>.delayed(Duration.zero);
    vm.syncHold('A');
    vm.addItem(popcorn.id);
    vm.syncSession('user-B');
    await Future<void>.delayed(Duration.zero);
    expect(vm.cart, isEmpty);
    vm.addItem(cola.id);
    vm.syncHold(
      'B',
    ); // First hold of the new account preserves its temporary cart.
    expect(vm.cart, {cola.id: 1});
    vm.syncSession(null);
    await Future<void>.delayed(Duration.zero);
    expect(vm.cart, isEmpty);
    expect(vm.catalog, isEmpty);
    vm.addItem(cola.id);
    expect(vm.cart, isEmpty);
  });

  test('an old refresh cannot restore cart/catalog after logout', () async {
    vm.syncSession('user-A');
    await Future<void>.delayed(Duration.zero);
    vm.addItem(popcorn.id);
    final response = Completer<List<CinemaConcession>>();
    source.respond = () => response.future;
    final loading = vm.load();
    vm.syncSession(null);
    await Future<void>.delayed(Duration.zero);
    response.complete([popcorn, cola]);
    await loading;
    expect(vm.cart, isEmpty);
    expect(vm.catalog, isEmpty);
  });

  testWidgets('rapid additions stop at ten and disable the add button', (
    tester,
  ) async {
    await showCatalog(tester, vm);
    for (var i = 0; i < 12; i++) {
      await tester.tap(find.byTooltip('Thêm Popcorn'));
    }
    await tester.pumpAndSettle();
    expect(vm.count, 10);
    expect(
      tester
          .widget<IconButton>(
            find.byWidgetPredicate(
              (widget) =>
                  widget is IconButton && widget.tooltip == 'Thêm Popcorn',
            ),
          )
          .onPressed,
      isNull,
    );
    expect(find.text('Tối đa 10 phần/món'), findsOneWidget);
  });

  testWidgets(
    'filter and cart round trip preserve both selections without a hold',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/concessions',
        routes: [
          GoRoute(
            path: '/concessions',
            builder: (_, _) => const ConcessionCatalogScreen(),
          ),
          GoRoute(
            path: '/cart',
            builder: (_, _) => const ConcessionCartScreen(),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(
        ChangeNotifierProvider.value(
          value: vm,
          child: MaterialApp.router(routerConfig: router),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byTooltip('Thêm Popcorn'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(ChoiceChip, 'Nước'));
      await tester.pumpAndSettle();
      await tester.tap(find.byTooltip('Thêm Cola'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Xem giỏ (2)'));
      await tester.pumpAndSettle();
      expect(find.text('50.000 ₫ × 1'), findsOneWidget);
      expect(find.text('25.000 ₫ × 1'), findsOneWidget);
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
      expect(vm.cart, {popcorn.id: 1, cola.id: 1});
      expect(find.text('Xem giỏ (2)'), findsOneWidget);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets(
    'catalog binds a new hold, including replacement of an existing screen',
    (tester) async {
      vm.addItem(popcorn.id);
      await showCatalog(tester, vm, currentHold: hold('A'));
      expect(vm.count, 1);
      await showCatalog(tester, vm, currentHold: hold('A'));
      expect(vm.count, 1);
      await showCatalog(tester, vm, currentHold: hold('B'));
      expect(vm.cart, isEmpty);
      expect(find.textContaining('lượt giữ ghế trước'), findsOneWidget);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets('valid hold hands off only selected IDs and quantities', (
    tester,
  ) async {
    final currentHold = hold('A');
    BookingSelection? selection;
    final router = GoRouter(
      initialLocation: '/cart',
      routes: [
        GoRoute(
          path: '/cart',
          builder: (_, _) => ConcessionCartScreen(hold: currentHold),
        ),
        GoRoute(
          path: '/booking-summary',
          builder: (_, state) {
            selection = state.extra as BookingSelection;
            return const Scaffold(body: Text('Summary fixture'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);
    vm.addItem(popcorn.id);
    vm.addItem(popcorn.id);
    vm.addItem(cola.id);
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: vm,
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    expect(selection!.holdId, 'A');
    expect(selection!.showtimeId, 'showtime');
    expect(selection!.showtimeSeatIds, ['seat']);
    expect(selection!.concessions.map((item) => item.concessionId), [
      popcorn.id,
      cola.id,
    ]);
    expect(selection!.concessions.map((item) => item.quantity), [2, 1]);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('direct cart navigation also clears a previous hold selection', (
    tester,
  ) async {
    vm.syncHold('A');
    vm.addItem(popcorn.id);
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: vm,
        child: MaterialApp(home: ConcessionCartScreen(hold: hold('B'))),
      ),
    );
    await tester.pumpAndSettle();
    expect(vm.cart, isEmpty);
    expect(find.text('Giỏ bắp nước trống'), findsOneWidget);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('expired hold cannot continue to booking summary', (
    tester,
  ) async {
    vm.addItem(popcorn.id);
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: vm,
        child: MaterialApp(
          home: ConcessionCartScreen(hold: hold('A', expired: true)),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Tiếp tục'));
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    expect(
      find.text('Vui lòng chọn và giữ ghế còn hạn trước khi tiếp tục.'),
      findsOneWidget,
    );
    expect(vm.cart, {popcorn.id: 1});
  });

  testWidgets(
    'refresh removal is visible even when the entire catalog is empty',
    (tester) async {
      vm.addItem(popcorn.id);
      await showCatalog(tester, vm);
      source.respond = () async => [];
      await tester.tap(find.byTooltip('Tải lại danh sách'));
      await tester.pumpAndSettle();
      expect(vm.cart, isEmpty);
      expect(find.textContaining('Đã bỏ món'), findsOneWidget);
      expect(find.text('Chưa có bắp/nước.'), findsOneWidget);
      await tester.tap(find.byTooltip('Đóng thông báo giỏ hàng'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Đã bỏ món'), findsNothing);
    },
  );
}
