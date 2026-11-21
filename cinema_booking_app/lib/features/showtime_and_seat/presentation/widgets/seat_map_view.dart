import 'package:flutter/material.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../domain/entities/seat_map.dart';
import 'seat_layout_template.dart';

/// Sơ đồ ghế dùng chung cho mọi kiểu phòng; [template] chỉ quyết định lối đi.
class SeatMapView extends StatelessWidget {
  const SeatMapView({
    super.key,
    required this.grid,
    required this.template,
    required this.selectedSeatIds,
    required this.onSeatTap,
  });

  final SeatGrid grid;
  final SeatLayoutTemplate template;
  final Set<String> selectedSeatIds;
  final ValueChanged<Seat> onSeatTap;

  static const _gap = 4.0;
  static const _minSeatSize = 26.0;
  static const _maxSeatSize = 38.0;

  /// Lối đi giữa các khối và nhãn hàng hai bên tính theo bội số cỡ ghế.
  static const _aisleFactor = 0.8;
  static const _rowLabelFactor = 0.9;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final aisleCount = template.aisleAfterSeatNumbers
          .where((number) => number > 0 && number < grid.columnCount)
          .length;
      final units =
          grid.columnCount + aisleCount * _aisleFactor + 2 * _rowLabelFactor;
      final gaps = (grid.columnCount + aisleCount + 1) * _gap;
      final seatSize = ((constraints.maxWidth - gaps) / units).clamp(
        _minSeatSize,
        _maxSeatSize,
      );

      return SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: ConstrainedBox(
          constraints: BoxConstraints(minWidth: constraints.maxWidth),
          child: Column(
            children: [
              for (final row in grid.rows)
                Padding(
                  padding: const EdgeInsets.only(bottom: _gap),
                  child: _buildRow(row, seatSize),
                ),
            ],
          ),
        ),
      );
    },
  );

  Widget _buildRow(SeatGridRow row, double seatSize) {
    final rowLabel = SizedBox(
      width: seatSize * _rowLabelFactor,
      child: Text(
        row.label,
        textAlign: TextAlign.center,
        style: const TextStyle(
          color: CinemaColors.muted,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    );

    final cells = <Widget>[rowLabel];
    for (var number = 1; number <= grid.columnCount; number++) {
      final seat = row.seatsByNumber[number];
      cells.add(
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: _gap / 2),
          child: seat == null
              ? SizedBox.square(dimension: seatSize)
              : SeatTile(
                  seat: seat,
                  size: seatSize,
                  selected: selectedSeatIds.contains(seat.showtimeSeatId),
                  onTap: () => onSeatTap(seat),
                ),
        ),
      );
      if (number < grid.columnCount && template.hasAisleAfter(number)) {
        cells.add(SizedBox(width: seatSize * _aisleFactor));
      }
    }
    cells.add(rowLabel);

    return Row(mainAxisSize: MainAxisSize.min, children: cells);
  }
}

/// Màu ghế theo loại và trạng thái; dùng chung cho ghế và chú thích.
class SeatPalette {
  const SeatPalette._(this.fill, this.border, this.text);

  final Color fill;
  final Color border;
  final Color text;

  static const standard = SeatPalette._(
    Color(0xFF2A2A2A),
    Color(0xFF5A4D4D),
    CinemaColors.text,
  );
  static const vip = SeatPalette._(
    Color(0x33FFB800),
    CinemaColors.warning,
    CinemaColors.warning,
  );
  static const selected = SeatPalette._(
    CinemaColors.red,
    CinemaColors.red,
    Colors.white,
  );
  static const pending = SeatPalette._(
    Color(0xFF263238),
    Color(0xFF455A64),
    Color(0xFF78909C),
  );
  static const unavailable = SeatPalette._(
    Color(0xFF181818),
    Color(0xFF2E2E2E),
    Color(0xFF555555),
  );

  static SeatPalette of(Seat seat, {required bool selected}) {
    // ViewModel chỉ đánh dấu selected cho ghế mình chọn/đang giữ (có thể đã
    // PENDING trên server), nên selected luôn được ưu tiên.
    if (selected) return SeatPalette.selected;
    return switch (seat.status) {
      SeatStatus.available =>
        seat.type == SeatType.vip ? SeatPalette.vip : SeatPalette.standard,
      SeatStatus.pending => SeatPalette.pending,
      SeatStatus.unavailable || SeatStatus.unknown => SeatPalette.unavailable,
    };
  }
}

class SeatTile extends StatelessWidget {
  const SeatTile({
    super.key,
    required this.seat,
    required this.size,
    required this.selected,
    required this.onTap,
  });

  final Seat seat;
  final double size;
  final bool selected;
  final VoidCallback onTap;

  String get _statusLabel => selected
      ? 'đang chọn'
      : switch (seat.status) {
          SeatStatus.available => 'còn trống',
          SeatStatus.pending => 'đang được giữ',
          SeatStatus.unavailable => 'đã đặt',
          SeatStatus.unknown => 'không khả dụng',
        };

  @override
  Widget build(BuildContext context) {
    final palette = SeatPalette.of(seat, selected: selected);
    final radius = BorderRadius.circular(size * 0.22);

    return Semantics(
      button: seat.isSelectable,
      selected: selected,
      label:
          'Ghế ${seat.code}'
          '${seat.type == SeatType.vip ? ' VIP' : ''}, $_statusLabel',
      excludeSemantics: true,
      child: Material(
        color: palette.fill,
        shape: RoundedRectangleBorder(
          borderRadius: radius,
          side: BorderSide(color: palette.border),
        ),
        child: InkWell(
          borderRadius: radius,
          // Ghế không khả dụng vẫn nhận chạm để ViewModel báo lý do.
          onTap: onTap,
          child: SizedBox.square(
            dimension: size,
            child: Padding(
              padding: const EdgeInsets.all(2),
              child: FittedBox(
                child: Text(
                  seat.code,
                  style: TextStyle(
                    color: palette.text,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    decoration: selected || seat.status == SeatStatus.available
                        ? null
                        : TextDecoration.lineThrough,
                    decorationColor: palette.text,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class SeatLegend extends StatelessWidget {
  const SeatLegend({super.key});

  static const _items = [
    ('Thường', SeatPalette.standard),
    ('VIP', SeatPalette.vip),
    ('Đang chọn', SeatPalette.selected),
    ('Đang giữ', SeatPalette.pending),
    ('Đã đặt', SeatPalette.unavailable),
  ];

  @override
  Widget build(BuildContext context) => Wrap(
    alignment: WrapAlignment.center,
    spacing: 14,
    runSpacing: 8,
    children: [
      for (final (label, palette) in _items)
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 14,
              height: 14,
              decoration: BoxDecoration(
                color: palette.fill,
                border: Border.all(color: palette.border),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
            const SizedBox(width: 5),
            Text(
              label,
              style: const TextStyle(color: CinemaColors.muted, fontSize: 11),
            ),
          ],
        ),
    ],
  );
}

class CinemaScreenIndicator extends StatelessWidget {
  const CinemaScreenIndicator({super.key});

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Container(
        height: 5,
        margin: const EdgeInsets.symmetric(horizontal: 24),
        decoration: BoxDecoration(
          color: CinemaColors.red,
          borderRadius: BorderRadius.circular(5),
          boxShadow: [
            BoxShadow(
              color: CinemaColors.red.withValues(alpha: 0.45),
              blurRadius: 14,
              offset: const Offset(0, 4),
            ),
          ],
        ),
      ),
      const SizedBox(height: 8),
      const Text(
        'MÀN HÌNH',
        style: TextStyle(
          color: CinemaColors.muted,
          fontSize: 11,
          letterSpacing: 3,
          fontWeight: FontWeight.w700,
        ),
      ),
    ],
  );
}
