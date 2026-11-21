import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../domain/entities/booking_summary.dart';
import '../view_models/booking_summary_view_model.dart';

class BookingSummaryScreen extends StatefulWidget {
  const BookingSummaryScreen({super.key, required this.selection});
  final BookingSelection selection;

  @override
  State<BookingSummaryScreen> createState() => _BookingSummaryScreenState();
}

class _BookingSummaryScreenState extends State<BookingSummaryScreen> {
  late final TextEditingController _voucherController;
  late BookingSelection _selection;
  late String _idempotencyKey;

  @override
  void initState() {
    super.initState();
    _selection = widget.selection;
    _voucherController = TextEditingController(
      text: _selection.voucherCode ?? '',
    );
    _idempotencyKey = _newIdempotencyKey();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      context.read<BookingSummaryViewModel>().preview(_selection);
    });
  }

  @override
  void dispose() {
    _voucherController.dispose();
    super.dispose();
  }

  void _refreshQuote({String? voucherCode}) {
    final viewModel = context.read<BookingSummaryViewModel>();

    if (viewModel.loading || viewModel.submitting) {
      return;
    }

    _selection = _selection.copyWith(
      voucherCode: voucherCode?.trim().toUpperCase(),
    );
    _idempotencyKey = _newIdempotencyKey();

    viewModel.preview(_selection);
  }

  Future<void> _createBooking() async {
    final created = await context.read<BookingSummaryViewModel>().create(
      _selection,
      idempotencyKey: _idempotencyKey,
    );
    if (!mounted) return;
    if (created == null) {
      final message = context.read<BookingSummaryViewModel>().error;
      if (message != null) showCinemaMessage(context, message, isError: true);
      return;
    }
    context.go('/booking-created', extra: created);
  }

  String _newIdempotencyKey() {
    final random = Random.secure();
    return '${DateTime.now().microsecondsSinceEpoch.toRadixString(36)}-'
        '${random.nextInt(1 << 32).toRadixString(36)}';
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<BookingSummaryViewModel>();
    final quote = viewModel.quote;
    return Scaffold(
      appBar: AppBar(title: const Text('Xác nhận đơn đặt vé')),
      body: viewModel.loading
          ? const Center(child: CircularProgressIndicator())
          : quote == null
          ? ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(
                  viewModel.error ?? 'Không thể tải thông tin đơn.',
                  style: const TextStyle(color: CinemaColors.red),
                ),
                const SizedBox(height: 12),
                _voucherCard(null, viewModel.submitting),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: viewModel.submitting
                      ? null
                      : () =>
                            _refreshQuote(voucherCode: _selection.voucherCode),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Thử lại'),
                ),
              ],
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 112),
              children: [
                _SectionCard(
                  title: 'Vé đã chọn',
                  icon: Icons.confirmation_num_outlined,
                  child: Column(
                    children: quote.ticketItems.map(_itemRow).toList(),
                  ),
                ),
                const SizedBox(height: 12),
                _SectionCard(
                  title: 'Bắp & nước',
                  icon: Icons.local_drink_outlined,
                  child: quote.concessionItems.isEmpty
                      ? const Align(
                          alignment: Alignment.centerLeft,
                          child: Text(
                            'Bạn chưa chọn bắp hoặc nước.',
                            style: TextStyle(color: CinemaColors.muted),
                          ),
                        )
                      : Column(
                          children: quote.concessionItems
                              .map(_itemRow)
                              .toList(),
                        ),
                ),
                const SizedBox(height: 12),
                _voucherCard(quote, viewModel.loading || viewModel.submitting),
                const SizedBox(height: 12),
                _priceCard(quote),
                const SizedBox(height: 16),
                const Text(
                  'Tổng tiền được tính lại bởi hệ thống khi bạn xác nhận đơn.',
                  style: TextStyle(color: CinemaColors.muted, fontSize: 12),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
      bottomNavigationBar: quote == null
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton(
                  onPressed: viewModel.loading || viewModel.submitting
                      ? null
                      : _createBooking,
                  child: viewModel.submitting
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text('Xác nhận · ${_vnd(quote.total)}'),
                ),
              ),
            ),
    );
  }

  Widget _voucherCard(BookingQuote? quote, bool busy) {
    final appliedVoucher = quote?.voucher;
    final hasVoucherCode = _selection.voucherCode?.trim().isNotEmpty ?? false;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Voucher',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _voucherController,
              enabled: !busy,
              textCapitalization: TextCapitalization.characters,
              decoration: const InputDecoration(
                hintText: 'Nhập mã giảm giá',
                prefixIcon: Icon(Icons.sell_outlined),
              ),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton(
                  onPressed: busy
                      ? null
                      : () {
                          final code = _voucherController.text
                              .trim()
                              .toUpperCase();

                          if (code.isEmpty) {
                            showCinemaMessage(
                              context,
                              'Vui lòng nhập mã voucher.',
                              isError: true,
                            );
                            return;
                          }

                          _refreshQuote(voucherCode: code);
                        },
                  child: const Text('Áp dụng'),
                ),
                if (hasVoucherCode)
                  TextButton(
                    onPressed: busy
                        ? null
                        : () {
                            _voucherController.clear();
                            _refreshQuote(voucherCode: '');
                          },
                    child: const Text('Gỡ voucher'),
                  ),
              ],
            ),
            if (appliedVoucher != null) ...[
              const SizedBox(height: 10),
              Text(
                'Đã áp dụng ${appliedVoucher.code}',
                style: const TextStyle(color: CinemaColors.warning),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _priceCard(BookingQuote quote) => _SectionCard(
    title: 'Tổng thanh toán',
    icon: Icons.receipt_long_outlined,
    child: Column(
      children: [
        _priceRow('Tiền vé', quote.ticketSubtotal),
        _priceRow('Bắp & nước', quote.concessionSubtotal),
        if (quote.discountAmount > 0)
          _priceRow(
            'Giảm giá',
            -quote.discountAmount,
            color: CinemaColors.warning,
          ),
        const Divider(),
        _priceRow('Tổng cộng', quote.total, total: true),
      ],
    ),
  );

  Widget _itemRow(BookingLineItem item) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 7),
    child: Row(
      children: [
        Expanded(child: Text(item.name)),
        Text(
          'x${item.quantity}',
          style: const TextStyle(color: CinemaColors.muted),
        ),
        const SizedBox(width: 16),
        Text(_vnd(item.lineTotal)),
      ],
    ),
  );

  Widget _priceRow(
    String label,
    int amount, {
    bool total = false,
    Color? color,
  }) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: total ? const TextStyle(fontWeight: FontWeight.w700) : null,
          ),
        ),
        Text(
          _vnd(amount),
          style: TextStyle(
            fontWeight: total ? FontWeight.w800 : null,
            color: color,
          ),
        ),
      ],
    ),
  );

  String _vnd(int amount) =>
      '${NumberFormat.decimalPattern('vi_VN').format(amount)} đ';
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.icon,
    required this.child,
  });
  final String title;
  final IconData icon;
  final Widget child;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: CinemaColors.warning),
              const SizedBox(width: 8),
              Text(title, style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const Divider(height: 24),
          child,
        ],
      ),
    ),
  );
}
