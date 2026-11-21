import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../domain/entities/customer_ticket.dart';
import '../view_models/customer_tickets_view_model.dart';

class MyTicketsScreen extends StatelessWidget {
  const MyTicketsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<CustomerTicketsViewModel>();
    return Scaffold(
      appBar: AppBar(title: const Text('Vé của tôi')),
      bottomNavigationBar: CinemaBottomNavigation(
        selectedIndex: 1,
        onItemSelected: (index) {
          if (index == 0) context.go('/movies');
          if (index == 2) context.go('/account');
        },
      ),
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
        retry: viewModel.loadTickets,
      );
    }
    if (viewModel.tickets.isEmpty) {
      return const AsyncStateView(
        icon: Icons.confirmation_num_outlined,
        message: 'Bạn chưa có vé đã thanh toán.',
      );
    }
    return RefreshIndicator(
      onRefresh: viewModel.loadTickets,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: viewModel.tickets.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (context, index) => _TicketCard(
          ticket: viewModel.tickets[index],
          onTap: () => context.push(
            '/tickets/${Uri.encodeComponent(viewModel.tickets[index].ticketId)}',
          ),
        ),
      ),
    );
  }
}

class _TicketCard extends StatelessWidget {
  const _TicketCard({required this.ticket, required this.onTap});

  final CustomerTicket ticket;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final isExpired = ticket.isExpired;
    final accentColor = isExpired ? CinemaColors.muted : CinemaColors.red;

    return Card(
      color: isExpired ? CinemaColors.surface.withValues(alpha: .72) : null,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 62,
                decoration: BoxDecoration(
                  color: accentColor.withValues(alpha: .16),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  Icons.confirmation_num_outlined,
                  color: accentColor,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      ticket.movieTitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      '${DateFormat('dd/MM/yyyy').format(ticket.showDate)} · ${_clock(ticket.startTime)}',
                      style: const TextStyle(color: CinemaColors.muted),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Phòng ${ticket.roomId} · ${ticket.seatLabels.join(', ')}',
                      style: const TextStyle(color: CinemaColors.muted),
                    ),
                    if (isExpired) ...[
                      const SizedBox(height: 6),
                      const Text(
                        'Đã hết hiệu lực',
                        style: TextStyle(
                          color: CinemaColors.muted,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: CinemaColors.muted),
            ],
          ),
        ),
      ),
    );
  }
}

String _clock(String value) =>
    value.length >= 5 ? value.substring(0, 5) : value;
