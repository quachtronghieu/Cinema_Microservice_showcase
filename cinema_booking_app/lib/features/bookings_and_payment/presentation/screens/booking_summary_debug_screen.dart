import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../domain/entities/booking_summary.dart';

/// Development-only form for opening the booking summary before seat and cart
/// screens are available in the mobile application.
class BookingSummaryDebugScreen extends StatefulWidget {
  const BookingSummaryDebugScreen({super.key});

  @override
  State<BookingSummaryDebugScreen> createState() =>
      _BookingSummaryDebugScreenState();
}

class _BookingSummaryDebugScreenState extends State<BookingSummaryDebugScreen> {
  final _formKey = GlobalKey<FormState>();
  final _holdId = TextEditingController();
  final _showtimeId = TextEditingController();
  final _seatIds = TextEditingController();
  final _concessionId = TextEditingController();
  final _concessionQuantity = TextEditingController(text: '1');
  final _voucherCode = TextEditingController();

  @override
  void dispose() {
    _holdId.dispose();
    _showtimeId.dispose();
    _seatIds.dispose();
    _concessionId.dispose();
    _concessionQuantity.dispose();
    _voucherCode.dispose();
    super.dispose();
  }

  void _openSummary() {
    if (!_formKey.currentState!.validate()) return;
    final concessionId = _concessionId.text.trim();
    final quantity = int.tryParse(_concessionQuantity.text.trim()) ?? 0;
    final selection = BookingSelection(
      holdId: _holdId.text.trim(),
      showtimeId: _showtimeId.text.trim(),
      showtimeSeatIds: _seatIds.text
          .split(',')
          .map((id) => id.trim())
          .where((id) => id.isNotEmpty)
          .toList(),
      concessions: concessionId.isEmpty
          ? const []
          : [
              ConcessionSelection(
                concessionId: concessionId,
                quantity: quantity,
              ),
            ],
      voucherCode: _voucherCode.text.trim(),
    );
    context.push('/booking-summary', extra: selection);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Dữ liệu test booking')),
    body: ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const Text(
          'Nhập các ID có thật từ SQL. Màn này chỉ dùng để test trước khi có luồng chọn ghế và bắp nước.',
          style: TextStyle(fontSize: 13),
        ),
        const SizedBox(height: 20),
        Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _holdId,
                decoration: const InputDecoration(labelText: 'Hold ID'),
                validator: (value) => value == null || value.trim().isEmpty
                    ? 'Hold ID is required.'
                    : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _showtimeId,
                decoration: const InputDecoration(labelText: 'Showtime ID'),
                validator: (value) => value == null || value.trim().isEmpty
                    ? 'Showtime ID là bắt buộc.'
                    : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _seatIds,
                decoration: const InputDecoration(
                  labelText: 'Showtime seat IDs',
                  hintText: 'SS000001, SS000002',
                ),
                validator: (value) => value == null || value.trim().isEmpty
                    ? 'Chọn ít nhất một ghế.'
                    : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _concessionId,
                decoration: const InputDecoration(
                  labelText: 'Concession ID (không bắt buộc)',
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _concessionQuantity,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Số lượng bắp/nước',
                ),
                validator: (value) {
                  if (_concessionId.text.trim().isEmpty) return null;
                  return (int.tryParse(value ?? '') ?? 0) > 0
                      ? null
                      : 'Số lượng phải lớn hơn 0.';
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _voucherCode,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: 'Voucher code (không bắt buộc)',
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _openSummary,
                  icon: const Icon(Icons.receipt_long_outlined),
                  label: const Text('Mở màn hình tóm tắt'),
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}
