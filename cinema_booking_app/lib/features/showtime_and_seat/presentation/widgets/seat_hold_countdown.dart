import 'dart:async';

import 'package:flutter/material.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../domain/entities/seat_hold.dart';

/// Đếm ngược thời gian giữ ghế theo [SeatHold.deadline]. Chỉ hiển thị;
/// việc nhả ghế khi hết hạn do ViewModel xử lý.
class SeatHoldCountdown extends StatefulWidget {
  const SeatHoldCountdown({super.key, required this.hold});

  final SeatHold hold;

  @override
  State<SeatHoldCountdown> createState() => _SeatHoldCountdownState();
}

class _SeatHoldCountdownState extends State<SeatHoldCountdown> {
  Timer? _ticker;
  Duration _remaining = Duration.zero;

  @override
  void initState() {
    super.initState();
    _start();
  }

  @override
  void didUpdateWidget(SeatHoldCountdown oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.hold.holdId != widget.hold.holdId) _start();
  }

  void _start() {
    _ticker?.cancel();
    _remaining = widget.hold.remainingAt(DateTime.now());
    if (_remaining > Duration.zero) {
      _ticker = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
    }
  }

  void _tick() {
    final remaining = widget.hold.remainingAt(DateTime.now());
    setState(() => _remaining = remaining);
    if (remaining == Duration.zero) _ticker?.cancel();
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final minutes = _remaining.inMinutes.toString().padLeft(2, '0');
    final seconds = (_remaining.inSeconds % 60).toString().padLeft(2, '0');
    // Dưới 1 phút đổi màu đỏ để khách chú ý.
    final color = _remaining.inSeconds < 60
        ? CinemaColors.red
        : CinemaColors.warning;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.timer_outlined, size: 16, color: color),
        const SizedBox(width: 6),
        Flexible(
          child: Text(
            'Giữ ghế còn $minutes:$seconds',
            overflow: TextOverflow.ellipsis,
            style: TextStyle(color: color, fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }
}
