import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../domain/entities/seat_hold.dart';
import '../../domain/entities/seat_map.dart';
import '../view_models/seat_selection_view_model.dart';
import '../widgets/seat_hold_countdown.dart';
import '../widgets/seat_layout_template.dart';
import '../widgets/seat_map_view.dart';

/// View Seat Map của một suất chiếu. ViewModel được tạo theo route nên mỗi
/// suất chiếu có lựa chọn riêng.
class SeatSelectionScreen extends StatelessWidget {
  const SeatSelectionScreen({super.key, required this.showtimeId});

  final String showtimeId;

  void _onSeatTap(BuildContext context, Seat seat) {
    final vm = context.read<SeatSelectionViewModel>();
    final message = switch (vm.toggle(seat)) {
      SeatToggleResult.unavailable =>
        seat.status == SeatStatus.pending
            ? 'Ghế ${seat.code} đang được giữ bởi khách khác.'
            : 'Ghế ${seat.code} không còn trống.',
      SeatToggleResult.limitReached =>
        'Bạn chỉ có thể chọn tối đa ${vm.maxSeats} ghế.',
      SeatToggleResult.differentRow =>
        'Chỉ được chọn các ghế trong cùng một hàng.',
      SeatToggleResult.notAdjacent =>
        'Vui lòng chọn ghế liền kề với ghế đã chọn.',
      SeatToggleResult.selected || SeatToggleResult.deselected => null,
    };
    if (message == null) return;
    _showMessage(context, message);
  }

  /// Giữ ghế trên server rồi mới sang bước chọn bắp nước. Dùng `push` để
  /// màn hình ghế (và hold) vẫn còn trong stack khi người dùng quay lại.
  Future<void> _onContinue(BuildContext context) async {
    final vm = context.read<SeatSelectionViewModel>();
    final error = await vm.holdSelectedSeats();
    if (!context.mounted) return;
    final hold = vm.state.hold;
    if (error != null) {
      _showMessage(context, error);
    } else if (hold != null) {
      context.push('/concessions', extra: hold);
    }
  }

  void _showMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SeatSelectionViewModel>();
    final state = vm.state;
    final seatMap = state.seatMap;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'CHỌN GHẾ',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900),
        ),
        actions: [
          IconButton(
            tooltip: 'Tải lại sơ đồ ghế',
            onPressed: state.isLoading ? null : () => vm.load(showtimeId),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 640),
          child: seatMap == null
              ? (state.errorMessage != null
                    ? AsyncStateView(
                        icon: Icons.error_outline,
                        message: state.errorMessage!,
                        retry: () => vm.load(showtimeId),
                      )
                    : const Center(child: CircularProgressIndicator()))
              : _SeatMapBody(
                  seatMap: seatMap,
                  selectedSeatIds: state.selectedSeatIds,
                  isRefreshing: state.isLoading,
                  refreshError: state.errorMessage,
                  onSeatTap: (seat) => _onSeatTap(context, seat),
                ),
        ),
      ),
      bottomNavigationBar: seatMap == null
          ? null
          : _SelectionBar(
              selectedSeats: state.selectedSeats,
              maxSeats: vm.maxSeats,
              isHolding: state.isHolding,
              hold: state.hold,
              holdExpired: state.holdExpired,
              onClear: vm.clearSelection,
              onContinue: () => _onContinue(context),
            ),
    );
  }
}

class _SeatMapBody extends StatelessWidget {
  const _SeatMapBody({
    required this.seatMap,
    required this.selectedSeatIds,
    required this.isRefreshing,
    required this.refreshError,
    required this.onSeatTap,
  });

  final SeatMap seatMap;
  final Set<String> selectedSeatIds;
  final bool isRefreshing;
  final String? refreshError;
  final ValueChanged<Seat> onSeatTap;

  @override
  Widget build(BuildContext context) {
    final grid = SeatGrid.fromSeats(seatMap.seats);
    final template = SeatLayoutTemplate.resolve(
      templateId: seatMap.templateId,
      seatsPerRow: grid.columnCount,
    );

    return ListView(
      padding: const EdgeInsets.fromLTRB(12, 0, 12, 24),
      children: [
        if (isRefreshing) const LinearProgressIndicator(minHeight: 2),
        _ShowtimeHeader(seatMap: seatMap, layout: template.kind),
        if (refreshError != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(
              refreshError!,
              textAlign: TextAlign.center,
              style: const TextStyle(color: CinemaColors.red, fontSize: 12),
            ),
          ),
        const CinemaScreenIndicator(),
        const SizedBox(height: 22),
        if (grid.isEmpty)
          const AsyncStateView(
            icon: Icons.event_seat_outlined,
            message: 'Suất chiếu chưa có sơ đồ ghế.',
          )
        else
          SeatMapView(
            grid: grid,
            template: template,
            selectedSeatIds: selectedSeatIds,
            onSeatTap: onSeatTap,
          ),
        const SizedBox(height: 20),
        const SeatLegend(),
      ],
    );
  }
}

class _ShowtimeHeader extends StatelessWidget {
  const _ShowtimeHeader({required this.seatMap, required this.layout});

  final SeatMap seatMap;
  final SeatLayoutKind layout;

  String? get _schedule {
    final date = seatMap.showDate;
    final time = seatMap.startTime?.split(':');
    final parts = [
      if (date != null)
        '${date.day.toString().padLeft(2, '0')}/'
            '${date.month.toString().padLeft(2, '0')}/${date.year}',
      if (time != null && time.length >= 2) '${time[0]}:${time[1]}',
      if (seatMap.roomId != null) 'Phòng ${seatMap.roomId}',
    ];
    return parts.isEmpty ? null : parts.join(' · ');
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 14),
    child: Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                seatMap.movieTitle.isEmpty ? 'Suất chiếu' : seatMap.movieTitle,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
              if (_schedule != null) ...[
                const SizedBox(height: 4),
                Text(
                  _schedule!,
                  style: const TextStyle(
                    color: CinemaColors.muted,
                    fontSize: 12,
                  ),
                ),
              ],
            ],
          ),
        ),
        Chip(
          visualDensity: VisualDensity.compact,
          label: Text(layout.label, style: const TextStyle(fontSize: 11)),
        ),
      ],
    ),
  );
}

class _SelectionBar extends StatelessWidget {
  const _SelectionBar({
    required this.selectedSeats,
    required this.maxSeats,
    required this.isHolding,
    required this.hold,
    required this.holdExpired,
    required this.onClear,
    required this.onContinue,
  });

  final List<Seat> selectedSeats;
  final int maxSeats;
  final bool isHolding;
  final SeatHold? hold;
  final bool holdExpired;
  final VoidCallback onClear;
  final VoidCallback onContinue;

  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: const BoxDecoration(
      color: CinemaColors.surface,
      border: Border(top: BorderSide(color: Color(0xFF332929))),
    ),
    child: SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (hold != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Center(child: SeatHoldCountdown(hold: hold!)),
              )
            else if (holdExpired)
              const Padding(
                padding: EdgeInsets.only(bottom: 6),
                child: Text(
                  'Hết thời gian giữ ghế. Bấm Tiếp tục để giữ lại.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: CinemaColors.red, fontSize: 12),
                ),
              ),
            Row(
              children: [
                Expanded(
                  child: Text(
                    selectedSeats.isEmpty
                        ? 'Chưa chọn ghế (tối đa $maxSeats)'
                        : 'Ghế (${selectedSeats.length}/$maxSeats): '
                              '${selectedSeats.map((seat) => seat.code).join(', ')}',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
                if (selectedSeats.isNotEmpty)
                  TextButton(
                    onPressed: isHolding ? null : onClear,
                    child: const Text('Bỏ chọn'),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: selectedSeats.isEmpty || isHolding ? null : onContinue,
              child: isHolding
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Tiếp tục'),
            ),
            const SizedBox(height: 4),
            const Text(
              'Ghế sẽ được giữ cho bạn trong 5 phút khi bấm Tiếp tục.',
              textAlign: TextAlign.center,
              style: TextStyle(color: CinemaColors.muted, fontSize: 11),
            ),
          ],
        ),
      ),
    ),
  );
}
