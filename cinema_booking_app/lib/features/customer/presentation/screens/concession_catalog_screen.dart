import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../view_models/concession_view_model.dart';
import '../widgets/cart_notice.dart';
import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../domain/entities/cinema_models.dart';
import '../../../showtime_and_seat/domain/entities/seat_hold.dart';
import '../../../showtime_and_seat/presentation/widgets/seat_hold_countdown.dart';

class ConcessionCatalogScreen extends StatefulWidget {
  const ConcessionCatalogScreen({super.key, this.hold});

  final SeatHold? hold;

  @override
  State<ConcessionCatalogScreen> createState() =>
      _ConcessionCatalogScreenState();
}

class _ConcessionCatalogScreenState extends State<ConcessionCatalogScreen> {
  bool _cartReady = false;
  bool _openingCart = false;
  SeatHold? get hold => widget.hold;

  @override
  void initState() {
    super.initState();
    _syncHold();
  }

  @override
  void didUpdateWidget(ConcessionCatalogScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.hold?.holdId != hold?.holdId) _syncHold();
  }

  void _syncHold() {
    _cartReady = false;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      context.read<ConcessionsViewModel>().syncHold(hold?.holdId);
      setState(() => _cartReady = true);
    });
  }

  Future<void> _openCart({bool skip = false}) async {
    if (_openingCart) return;
    setState(() => _openingCart = true);
    try {
      if (skip) context.read<ConcessionsViewModel>().clearCart();
      await context.push<void>('/cart', extra: hold);
    } finally {
      if (mounted) setState(() => _openingCart = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_cartReady) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    final vm = context.watch<ConcessionsViewModel>();
    final items = vm.filteredCatalog;
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'BẮP & NƯỚC',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900),
        ),
        actions: [
          IconButton(
            tooltip: 'Tải lại danh sách',
            icon: const Icon(Icons.refresh),
            onPressed: vm.busy ? null : () => vm.load(),
          ),
        ],
        bottom: const PreferredSize(
          preferredSize: Size.fromHeight(1),
          child: Divider(height: 1, color: Color(0xFF332929)),
        ),
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 390),
          child: Column(
            children: [
              const CartNotice(),
              if (hold != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                  child: SeatHoldCountdown(hold: hold!),
                ),
              if (vm.error != null && vm.catalog.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                  child: Column(
                    children: [
                      Text(vm.error!, textAlign: TextAlign.center),
                      TextButton(
                        onPressed: vm.busy ? null : () => vm.load(),
                        child: const Text('Retry'),
                      ),
                    ],
                  ),
                ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: ConcessionFilter.values.map((filter) {
                    final label = switch (filter) {
                      ConcessionFilter.all => 'Tất cả',
                      ConcessionFilter.popcorn => 'Bắp',
                      ConcessionFilter.drink => 'Nước',
                    };

                    return ChoiceChip(
                      label: Text(label),
                      shape: const StadiumBorder(),
                      selected: vm.filter == filter,
                      onSelected: (selected) {
                        if (selected) {
                          vm.setFilter(filter);
                        }
                      },
                    );
                  }).toList(),
                ),
              ),
              Expanded(
                child: vm.busy && vm.catalog.isEmpty
                    ? const Center(child: CircularProgressIndicator())
                    : vm.error != null && vm.catalog.isEmpty
                    ? AsyncStateView(
                        message: vm.error!,
                        retry: vm.load,
                        icon: Icons.error_outline,
                      )
                    : vm.catalog.isEmpty
                    ? const AsyncStateView(message: 'Chưa có bắp/nước.')
                    : items.isEmpty
                    ? const AsyncStateView(
                        message: 'Không có món thuộc loại đã chọn.',
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: items.length,
                        itemBuilder: (context, index) =>
                            _ConcessionTile(items[index]),
                      ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _openingCart ? null : () => _openCart(skip: true),
                  child: const Text('Bỏ qua'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton(
                  onPressed: _openingCart ? null : () => _openCart(),
                  child: Text('Xem giỏ (${vm.count})'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConcessionTile extends StatelessWidget {
  const _ConcessionTile(this.item);
  final CinemaConcession item;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<ConcessionsViewModel>();
    final quantity = vm.cart[item.id] ?? 0;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            CinemaPoster(item.image, width: 86, height: 86),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.name,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  Text(
                    item.description,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: CinemaColors.muted,
                      fontSize: 12,
                    ),
                  ),
                  Text(
                    formatVnd(item.price),
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  if (quantity >= ConcessionsViewModel.maxQuantityPerItem)
                    const Text('Tối đa 10 phần/món'),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      if (quantity > 0)
                        IconButton(
                          tooltip: 'Giảm ${item.name}',
                          onPressed: () => vm.decreaseItem(item.id),
                          icon: const Icon(Icons.remove_circle_outline),
                        ),
                      if (quantity > 0) Text('$quantity'),
                      IconButton(
                        tooltip: 'Thêm ${item.name}',
                        onPressed:
                            quantity >= ConcessionsViewModel.maxQuantityPerItem
                            ? null
                            : () => vm.addItem(item.id),
                        icon: const Icon(
                          Icons.add_circle,
                          color: CinemaColors.red,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
