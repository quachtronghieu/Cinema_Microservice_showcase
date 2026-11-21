import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/entities/booking_summary.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_cart_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';

import 'add_concession_to_cart_test.dart' as fixtures;

void main() {
  late ConcessionsViewModel vm;
  late GoRouter router;
  BookingSelection? selection;

  Future<void> show(
    WidgetTester tester, {
    bool cart = false,
    SeatHold? hold,
  }) async {
    router = GoRouter(
      initialLocation: cart ? '/cart' : '/concessions',
      routes: [
        GoRoute(
          path: '/concessions',
          builder: (_, _) => ConcessionCatalogScreen(hold: hold),
        ),
        GoRoute(
          path: '/cart',
          builder: (_, state) => ConcessionCartScreen(
            hold: cart ? hold : state.extra as SeatHold?,
          ),
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
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: vm,
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() async {
    selection = null;
    vm = ConcessionsViewModel(fixtures.CartRepository());
    await vm.load();
  });
  tearDown(() => vm.dispose());

  for (final cart in [false, true]) {
    testWidgets(
      'rapid decreases use latest quantity on ${cart ? 'cart' : 'catalog'}',
      (tester) async {
        vm.setQuantity(fixtures.popcorn.id, 3);
        vm.addItem(fixtures.cola.id);
        await show(tester, cart: cart);
        await tester.tap(find.byTooltip('Giảm Popcorn'));
        await tester.tap(find.byTooltip('Giảm Popcorn'));
        await tester.pumpAndSettle();
        expect(vm.cart, {fixtures.popcorn.id: 1, fixtures.cola.id: 1});
        expect(vm.count, 2);
        expect(vm.foodSubtotal, 75000);
      },
    );
  }

  testWidgets(
    'mixed increase/decrease before rebuild preserves the correct count',
    (tester) async {
      vm.setQuantity(fixtures.popcorn.id, 2);
      await show(tester, cart: true);
      await tester.tap(find.byTooltip('Tăng Popcorn'));
      await tester.tap(find.byTooltip('Giảm Popcorn'));
      await tester.pumpAndSettle();
      expect(vm.cart[fixtures.popcorn.id], 2);
    },
  );

  testWidgets(
    'removing the final unit removes the line and allows ticket-only handoff',
    (tester) async {
      final hold = fixtures.hold('H1');
      vm.addItem(fixtures.popcorn.id);
      await show(tester, cart: true, hold: hold);
      await tester.tap(find.byTooltip('Giảm Popcorn'));
      await tester.tap(find.byTooltip('Giảm Popcorn'));
      await tester.pumpAndSettle();
      expect(vm.cart, isEmpty);
      expect(vm.count, 0);
      expect(vm.foodSubtotal, 0);
      expect(find.text('Giỏ bắp nước trống'), findsOneWidget);
      await tester.ensureVisible(find.text('Tiếp tục'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Tiếp tục'));
      await tester.pumpAndSettle();
      expect(selection!.holdId, hold.holdId);
      expect(selection!.showtimeSeatIds, hold.seatIds);
      expect(selection!.concessions, isEmpty);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets('decreasing from the cap enables add again', (tester) async {
    vm.setQuantity(fixtures.popcorn.id, 10);
    await show(tester, cart: true);
    final plus = find.byWidgetPredicate(
      (w) => w is IconButton && w.tooltip == 'Tăng Popcorn',
    );
    expect(tester.widget<IconButton>(plus).onPressed, isNull);
    await tester.tap(find.byTooltip('Giảm Popcorn'));
    await tester.pumpAndSettle();
    expect(tester.widget<IconButton>(plus).onPressed, isNotNull);
    await tester.tap(find.byTooltip('Tăng Popcorn'));
    await tester.pumpAndSettle();
    expect(vm.count, 10);
  });

  for (final populated in [false, true]) {
    testWidgets(
      'skip with populated=$populated hands off an empty cart and the same hold',
      (tester) async {
        final hold = fixtures.hold('H1');
        if (populated) {
          vm.setQuantity(fixtures.popcorn.id, 2);
          vm.addItem(fixtures.cola.id);
        }
        await show(tester, hold: hold);
        await tester.tap(find.text('Bỏ qua'));
        await tester.pumpAndSettle();
        expect(vm.cart, isEmpty);
        expect(vm.foodSubtotal, 0);
        expect(
          tester
              .widget<ConcessionCartScreen>(find.byType(ConcessionCartScreen))
              .hold,
          same(hold),
        );
        await tester.ensureVisible(find.text('Tiếp tục'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Tiếp tục'));
        await tester.pumpAndSettle();
        expect(selection!.concessions, isEmpty);
        expect(selection!.holdId, hold.holdId);
        expect(selection!.showtimeSeatIds, hold.seatIds);
        await tester.pumpWidget(const SizedBox.shrink());
      },
    );
  }

  testWidgets(
    'double skip pushes only one cart and back does not restore discarded items',
    (tester) async {
      vm.addItem(fixtures.popcorn.id);
      await show(tester);
      await tester.tap(find.text('Bỏ qua'));
      await tester.tap(find.text('Bỏ qua'));
      await tester.pumpAndSettle();
      router.pop();
      await tester.pumpAndSettle();
      expect(find.byType(ConcessionCatalogScreen), findsOneWidget);
      expect(find.byType(ConcessionCartScreen), findsNothing);
      expect(vm.cart, isEmpty);
      await tester.tap(find.byTooltip('Thêm Cola'));
      await tester.pumpAndSettle();
      expect(vm.cart, {fixtures.cola.id: 1});
    },
  );

  testWidgets('skip without a hold does not enable booking', (tester) async {
    vm.addItem(fixtures.popcorn.id);
    await show(tester);
    await tester.tap(find.text('Bỏ qua'));
    await tester.pumpAndSettle();
    expect(vm.cart, isEmpty);
    expect(
      tester
          .widget<ElevatedButton>(
            find.widgetWithText(ElevatedButton, 'Tiếp tục'),
          )
          .onPressed,
      isNull,
    );
    expect(selection, isNull);
  });

  testWidgets('skip with an expired hold cannot bypass expiry', (tester) async {
    await show(tester, hold: fixtures.hold('expired', expired: true));
    await tester.tap(find.text('Bỏ qua'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    expect(selection, isNull);
    expect(
      find.text('Vui lòng chọn và giữ ghế còn hạn trước khi tiếp tục.'),
      findsOneWidget,
    );
  });
}
