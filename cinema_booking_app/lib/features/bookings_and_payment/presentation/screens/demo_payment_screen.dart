import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../domain/entities/booking_summary.dart';
import '../../domain/entities/demo_payment.dart';
import '../view_models/demo_payment_view_model.dart';

class DemoPaymentScreen extends StatelessWidget {
  const DemoPaymentScreen({super.key, required this.booking});

  final CreatedBooking booking;

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<DemoPaymentViewModel>();
    final result = viewModel.result;
    return Scaffold(
      appBar: AppBar(title: const Text('Thanh toán demo')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: result == null
              ? _PaymentOptions(booking: booking, viewModel: viewModel)
              : _PaymentResult(booking: booking, result: result),
        ),
      ),
    );
  }
}

class _PaymentOptions extends StatelessWidget {
  const _PaymentOptions({required this.booking, required this.viewModel});

  final CreatedBooking booking;
  final DemoPaymentViewModel viewModel;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      const Icon(Icons.account_balance_wallet_outlined, size: 56),
      const SizedBox(height: 16),
      Text(
        'Xử lý thanh toán',
        style: Theme.of(context).textTheme.headlineSmall,
        textAlign: TextAlign.center,
      ),
      const SizedBox(height: 8),
      const Text(
        'Chọn kết quả để kiểm thử luồng thanh toán. Hệ thống sẽ xử lý trên server.',
        textAlign: TextAlign.center,
        style: TextStyle(color: CinemaColors.muted),
      ),
      const SizedBox(height: 8),
      const Text(
        'Đơn sẽ tự hủy sau 5 phút nếu chưa thanh toán.',
        textAlign: TextAlign.center,
        style: TextStyle(color: CinemaColors.warning),
      ),
      const SizedBox(height: 28),
      Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            children: [
              _Row(label: 'Mã đơn', value: booking.bookingId),
              const Divider(height: 24),
              _Row(
                label: 'Số tiền',
                value: _vnd(booking.totalAmount),
                emphasis: true,
              ),
            ],
          ),
        ),
      ),
      if (viewModel.error != null) ...[
        const SizedBox(height: 18),
        _ErrorMessage(message: viewModel.error!),
      ],
      const SizedBox(height: 28),
      FilledButton.icon(
        onPressed: viewModel.submitting
            ? null
            : () => viewModel.process(
                bookingId: booking.bookingId,
                outcome: DemoPaymentOutcome.success,
              ),
        style: FilledButton.styleFrom(backgroundColor: Colors.green),
        icon: viewModel.submitting
            ? const SizedBox(
                height: 18,
                width: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : const Icon(Icons.check_circle_outline),
        label: const Text('Mô phỏng thanh toán thành công'),
      ),
      const SizedBox(height: 12),
      OutlinedButton.icon(
        onPressed: viewModel.submitting
            ? null
            : () => viewModel.process(
                bookingId: booking.bookingId,
                outcome: DemoPaymentOutcome.failed,
              ),
        style: OutlinedButton.styleFrom(foregroundColor: CinemaColors.red),
        icon: const Icon(Icons.cancel_outlined),
        label: const Text('Mô phỏng thanh toán thất bại'),
      ),
    ],
  );
}

class _PaymentResult extends StatelessWidget {
  const _PaymentResult({required this.booking, required this.result});

  final CreatedBooking booking;
  final DemoPaymentResult result;

  @override
  Widget build(BuildContext context) {
    final success = result.isSuccessful;
    final color = success ? Colors.green : CinemaColors.red;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        CircleAvatar(
          radius: 40,
          backgroundColor: color,
          child: Icon(
            success ? Icons.check_rounded : Icons.close_rounded,
            size: 48,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 24),
        Text(
          success ? 'Thanh toán thành công' : 'Thanh toán thất bại',
          style: Theme.of(context).textTheme.headlineSmall,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          result.message,
          textAlign: TextAlign.center,
          style: const TextStyle(color: CinemaColors.muted),
        ),
        const SizedBox(height: 28),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              children: [
                _Row(label: 'Mã đơn', value: booking.bookingId),
                const Divider(height: 24),
                _Row(label: 'Mã giao dịch', value: result.paymentId),
                const Divider(height: 24),
                _Row(
                  label: 'Số tiền',
                  value: _vnd(result.amount),
                  emphasis: true,
                ),
                if (success && result.ticketId != null) ...[
                  const Divider(height: 24),
                  _Row(label: 'Mã vé', value: result.ticketId!),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 28),
        if (success)
          FilledButton.icon(
            onPressed: result.ticketId == null
                ? null
                : () => context.go(
                    '/tickets/${Uri.encodeComponent(result.ticketId!)}',
                  ),
            icon: const Icon(Icons.confirmation_num_outlined),
            label: const Text('Xem vé đã phát hành'),
          )
        else ...[
          FilledButton(
            onPressed: () => context.go('/account'),
            child: const Text('Về tài khoản'),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: () => context.go('/movies'),
            child: const Text('Đặt vé mới'),
          ),
        ],
      ],
    );
  }
}

class _ErrorMessage extends StatelessWidget {
  const _ErrorMessage({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: CinemaColors.red.withValues(alpha: .12),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Text(message, style: const TextStyle(color: CinemaColors.red)),
  );
}

class _Row extends StatelessWidget {
  const _Row({required this.label, required this.value, this.emphasis = false});

  final String label;
  final String value;
  final bool emphasis;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Expanded(
        child: Text(label, style: const TextStyle(color: CinemaColors.muted)),
      ),
      Text(
        value,
        style: TextStyle(
          fontWeight: emphasis ? FontWeight.w800 : FontWeight.w600,
        ),
      ),
    ],
  );
}

String _vnd(int amount) =>
    '${NumberFormat.decimalPattern('vi_VN').format(amount)} đ';
