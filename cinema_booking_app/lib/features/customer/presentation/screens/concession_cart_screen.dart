import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_widgets.dart';
import '../view_models/concession_view_model.dart';
import '../widgets/cart_notice.dart';
import '../../../showtime_and_seat/domain/entities/seat_hold.dart';
import '../../../showtime_and_seat/presentation/widgets/seat_hold_countdown.dart';
import '../../../bookings_and_payment/domain/entities/booking_summary.dart';

class ConcessionCartScreen extends StatefulWidget {
  const ConcessionCartScreen({super.key, this.hold});

  final SeatHold? hold;

  @override
  State<ConcessionCartScreen> createState() => _ConcessionCartScreenState();
}

class _ConcessionCartScreenState extends State<ConcessionCartScreen> {
  bool _previewStarted = false;
  bool _openingSummary = false;
  SeatHold? get hold => widget.hold;

  @override
  void initState() {
    super.initState();
    _scheduleTicketPreview();
  }

  @override
  void didUpdateWidget(ConcessionCartScreen oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.hold?.holdId != widget.hold?.holdId ||
        oldWidget.hold?.deadline != widget.hold?.deadline) {
      _scheduleTicketPreview();
    }
  }

  void _scheduleTicketPreview() {
    _previewStarted = false;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }

      setState(() => _previewStarted = true);
      final vm = context.read<ConcessionsViewModel>();
      vm.syncHold(hold?.holdId);
      vm.loadTicketPreview(hold);
    });
  }

  Future<void> _openBookingSummary() async {
    if (_openingSummary) {
      return;
    }

    final currentHold = hold;

    if (currentHold == null || currentHold.isExpiredAt(DateTime.now())) {
      showCinemaMessage(
        context,
        'Vui lòng chọn và giữ ghế còn hạn trước khi tiếp tục.',
        isError: true,
      );
      return;
    }

    final vm = context.read<ConcessionsViewModel>();

    final selection = BookingSelection(
      holdId: currentHold.holdId,
      showtimeId: currentHold.showtimeId,
      showtimeSeatIds: List<String>.of(currentHold.seatIds),
      concessions: vm.cart.entries
          .map(
            (item) => ConcessionSelection(
              concessionId: item.key,
              quantity: item.value,
            ),
          )
          .toList(),
    );

    setState(() => _openingSummary = true);

    try {
      await context.push<void>('/booking-summary', extra: selection);
    } finally {
      if (mounted) {
        setState(() => _openingSummary = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_previewStarted) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    final vm = context.watch<ConcessionsViewModel>();
    final selected = vm.catalog
        .where((item) => vm.cart.containsKey(item.id))
        .toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Giỏ hàng')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const CartNotice(),
          if (hold != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: SeatHoldCountdown(hold: hold!),
            ),
          const SectionHeading('Bắp & nước'),
          if (selected.isEmpty)
            const AsyncStateView(message: 'Giỏ bắp nước trống')
          else
            ...selected.map((item) {
              final quantity = vm.cart[item.id]!;

              return Card(
                child: ListTile(
                  leading: CinemaPoster(item.image, width: 52, height: 52),
                  title: Text(item.name),
                  subtitle: Text('${formatVnd(item.price)} × $quantity'),
                  trailing: SizedBox(
                    width: 124,
                    child: Row(
                      children: [
                        IconButton(
                          tooltip: 'Giảm ${item.name}',
                          onPressed: () => vm.decreaseItem(item.id),
                          icon: const Icon(Icons.remove, size: 18),
                        ),
                        Text('$quantity'),
                        IconButton(
                          tooltip: 'Tăng ${item.name}',
                          onPressed:
                              quantity >=
                                  ConcessionsViewModel.maxQuantityPerItem
                              ? null
                              : () => vm.addItem(item.id),
                          icon: const Icon(Icons.add, size: 18),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
          TextButton.icon(
            onPressed: () {
              if (context.canPop()) {
                context.pop();
              } else {
                context.go('/concessions', extra: hold);
              }
            },
            icon: const Icon(Icons.add),
            label: const Text('Thêm món'),
          ),
          const SectionHeading('Tạm tính'),

          if (hold == null)
            const Text('Chọn và giữ ghế để xem tổng tạm tính gồm tiền vé.')
          else if (!_previewStarted || vm.ticketLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: LinearProgressIndicator(),
            )
          else if (vm.ticketError != null) ...[
            Text(vm.ticketError!),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: () => context
                    .read<ConcessionsViewModel>()
                    .loadTicketPreview(hold),
                icon: const Icon(Icons.refresh),
                label: const Text('Tải lại giá ghế'),
              ),
            ),
          ] else if (vm.ticketSubtotal != null)
            PriceLine('Tiền vé', vm.ticketSubtotal!),

          PriceLine('Bắp & nước', vm.foodSubtotal),

          if (hold != null &&
              _previewStarted &&
              !vm.ticketLoading &&
              vm.ticketError == null &&
              vm.previewTotal != null) ...[
            const Divider(),
            PriceLine('Tổng tạm tính', vm.previewTotal!, total: true),
          ],

          const SizedBox(height: 8),
          const Text(
            'Số tiền tạm tính chưa gồm giảm giá voucher. '
            'Tổng thanh toán sẽ được hệ thống xác nhận khi tạo đơn.',
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _openingSummary || hold == null
                  ? null
                  : _openBookingSummary,
              child: const Text('Tiếp tục'),
            ),
          ),
        ],
      ),
    );
  }
}
