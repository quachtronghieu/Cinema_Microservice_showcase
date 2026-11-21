import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'package:cinema_booking_app/features/customer/domain/entities/cinema_models.dart';
import 'package:cinema_booking_app/features/customer/domain/repositories/concession_repository.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_cart_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/screens/concession_catalog_screen.dart';
import 'package:cinema_booking_app/features/customer/presentation/view_models/concession_view_model.dart';

class _FakeConcessionRepository implements ConcessionRepository {
  @override
  Future<List<CinemaConcession>> listConcessions() async => [
    const CinemaConcession(
      id: 'C01',
      type: 'PC',
      name: 'Bắp rang',
      description: 'Bắp rang thử nghiệm',
      price: 20000,
      image: '',
    ),
  ];
}

void main() {
  testWidgets('Thêm món, tăng giảm và xóa khỏi giỏ', (tester) async {
    final vm = ConcessionsViewModel(_FakeConcessionRepository());
    await vm.load();

    final router = GoRouter(
      initialLocation: '/concessions',
      routes: [
        GoRoute(
          path: '/concessions',
          builder: (_, state) => const ConcessionCatalogScreen(),
        ),
        GoRoute(
          path: '/cart',
          builder: (_, state) => const ConcessionCartScreen(),
        ),
      ],
    );

    try {
      await tester.pumpWidget(
        ChangeNotifierProvider<ConcessionsViewModel>.value(
          value: vm,
          child: MaterialApp.router(routerConfig: router),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Bắp rang'), findsOneWidget);

      await tester.tap(find.byTooltip('Thêm Bắp rang'));
      await tester.pumpAndSettle();

      expect(find.text('Xem giỏ (1)'), findsOneWidget);

      await tester.tap(find.text('Xem giỏ (1)'));
      await tester.pumpAndSettle();

      expect(find.text('Giỏ hàng'), findsOneWidget);
      expect(find.text('20.000 ₫ × 1'), findsOneWidget);

      await tester.tap(find.byTooltip('Tăng Bắp rang'));
      await tester.pumpAndSettle();

      expect(find.text('20.000 ₫ × 2'), findsOneWidget);
      expect(vm.foodSubtotal, 40000);

      await tester.tap(find.byTooltip('Giảm Bắp rang'));
      await tester.pumpAndSettle();

      expect(find.text('20.000 ₫ × 1'), findsOneWidget);

      await tester.tap(find.byTooltip('Giảm Bắp rang'));
      await tester.pumpAndSettle();

      expect(find.text('Giỏ bắp nước trống'), findsOneWidget);
      expect(vm.count, 0);
      expect(vm.foodSubtotal, 0);
    } finally {
      await tester.pumpWidget(const SizedBox.shrink());
      router.dispose();
      vm.dispose();
    }
  });
}
