import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../domain/entities/customer_ticket.dart';
import '../view_models/customer_tickets_view_model.dart';

class CustomerTicketDetailScreen extends StatelessWidget {
  const CustomerTicketDetailScreen({super.key, required this.ticketId});

  final String ticketId;

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<CustomerTicketsViewModel>();
    return Scaffold(
      appBar: AppBar(title: const Text('Vé điện tử')),
      body: _body(context, viewModel),
    );
  }

  Widget _body(BuildContext context, CustomerTicketsViewModel viewModel) {
    if (viewModel.loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (viewModel.error != null) {
      return AsyncStateView(
        icon: Icons.error_outline,
        message: viewModel.error!,
        retry: () => viewModel.loadTicket(ticketId),
      );
    }
    final ticket = viewModel.ticket;
    if (ticket == null) {
      return const AsyncStateView(message: 'Không tìm thấy vé.');
    }
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _IssuedTicketCard(ticket: ticket),
          const SizedBox(height: 20),
          Text(
            ticket.isExpired
                ? 'Suất chiếu đã qua. Vé chỉ còn dùng để xem thông tin.'
                : ticket.checkedIn
                ? 'Vé đã được sử dụng.'
                : 'Xuất trình mã vé khi check-in.',
            textAlign: TextAlign.center,
            style: const TextStyle(color: CinemaColors.muted),
          ),
          const SizedBox(height: 20),
          OutlinedButton(
            onPressed: () => context.go('/tickets'),
            child: const Text('Xem tất cả vé'),
          ),
        ],
      ),
    );
  }
}

class _IssuedTicketCard extends StatelessWidget {
  const _IssuedTicketCard({required this.ticket});

  final CustomerTicket ticket;

  @override
  Widget build(BuildContext context) => Card(
    clipBehavior: Clip.antiAlias,
    child: Column(
      children: [
        Container(
          width: double.infinity,
          color: ticket.checkedIn || ticket.isExpired
              ? CinemaColors.muted
              : CinemaColors.red,
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
          child: const Row(
            children: [
              Icon(Icons.confirmation_num, color: Colors.white),
              SizedBox(width: 10),
              Text(
                'VÉ ĐIỆN TỬ ĐÃ PHÁT HÀNH',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                ticket.movieTitle,
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 20),
              _TicketRow(
                label: 'Ngày chiếu',
                value: DateFormat(
                  'EEEE, dd/MM/yyyy',
                  'vi_VN',
                ).format(ticket.showDate),
              ),
              _TicketRow(label: 'Giờ chiếu', value: _clock(ticket.startTime)),
              _TicketRow(label: 'Phòng', value: ticket.roomId),
              _TicketRow(label: 'Ghế', value: ticket.seatLabels.join(', ')),
              if (ticket.concessionItems.isNotEmpty) ...[
                const Divider(height: 30),
                const Text(
                  'Bắp nước',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 6),
                ...ticket.concessionItems.map(
                  (item) =>
                      _TicketRow(label: item.name, value: 'x${item.quantity}'),
                ),
              ],
              const Divider(height: 30),
              _TicketRow(
                label: 'Mã vé',
                value: ticket.ticketId,
                emphasis: true,
              ),
              _TicketRow(label: 'Mã đơn', value: ticket.bookingId),
              _TicketRow(
                label: 'Tổng thanh toán',
                value: formatVnd(ticket.totalAmount),
                emphasis: true,
              ),
            ],
          ),
        ),
      ],
    ),
  );
}

class _TicketRow extends StatelessWidget {
  const _TicketRow({
    required this.label,
    required this.value,
    this.emphasis = false,
  });

  final String label;
  final String value;
  final bool emphasis;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 6),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(label, style: const TextStyle(color: CinemaColors.muted)),
        ),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: TextStyle(
              fontWeight: emphasis ? FontWeight.w800 : FontWeight.w600,
            ),
          ),
        ),
      ],
    ),
  );
}

String _clock(String value) =>
    value.length >= 5 ? value.substring(0, 5) : value;
