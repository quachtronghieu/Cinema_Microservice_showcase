import '../../domain/entities/seat_map.dart';

enum SeatLayoutKind {
  /// Một khối ghế liền nhau (ví dụ 12 ghế/hàng).
  standard('Tiêu chuẩn'),

  /// Hai khối, một lối đi chính giữa (ví dụ 14 ghế/hàng: 7 + 7).
  centerAisle('Lối đi giữa'),

  /// Ba khối, hai lối đi phụ (ví dụ 10 ghế/hàng: 2 + 6 + 2).
  threeBlocks('Ba khối');

  const SeatLayoutKind(this.label);

  final String label;
}

/// Mô tả vị trí lối đi của một kiểu phòng.
///
/// Backend chưa lưu thông tin lối đi, nên lối đi được đặt sau một
/// `seat_number` cụ thể; vị trí cột của ghế luôn lấy từ `seat_number`.
class SeatLayoutTemplate {
  const SeatLayoutTemplate({
    required this.kind,
    this.aisleAfterSeatNumbers = const {},
  });

  factory SeatLayoutTemplate.forKind(SeatLayoutKind kind, int seatsPerRow) {
    switch (kind) {
      case SeatLayoutKind.standard:
        return SeatLayoutTemplate(kind: kind);
      case SeatLayoutKind.centerAisle:
        return SeatLayoutTemplate(
          kind: kind,
          aisleAfterSeatNumbers: {seatsPerRow ~/ 2},
        );
      case SeatLayoutKind.threeBlocks:
        final sideBlock = seatsPerRow ~/ 4;
        return SeatLayoutTemplate(
          kind: kind,
          aisleAfterSeatNumbers: {sideBlock, seatsPerRow - sideBlock},
        );
    }
  }

  /// Bố cục hiển thị theo `template_id`. Dữ liệu phòng không lưu lối đi nên vị
  /// trí lối đi này do app quy ước, cần đối chiếu thiết kế (scope S-04).
  ///
  /// TM000003 có 8 hàng × 10 ghế nên chia thành 2 + 6 + 2.
  static const Map<String, SeatLayoutKind> knownTemplateKinds = {
    'TM000001': SeatLayoutKind.standard,
    'TM000002': SeatLayoutKind.centerAisle,
    'TM000003': SeatLayoutKind.threeBlocks,
  };

  /// Chọn template cho một phòng theo [knownTemplateKinds]; `template_id`
  /// lạ hoặc thiếu thì suy theo số ghế lớn nhất trong một hàng.
  factory SeatLayoutTemplate.resolve({
    required String? templateId,
    required int seatsPerRow,
  }) {
    final kind = knownTemplateKinds[templateId] ?? kindForRowWidth(seatsPerRow);
    return SeatLayoutTemplate.forKind(kind, seatsPerRow);
  }

  static SeatLayoutKind kindForRowWidth(int seatsPerRow) {
    if (seatsPerRow == 9) return SeatLayoutKind.standard;
    if (seatsPerRow == 10) return SeatLayoutKind.centerAisle;
    return SeatLayoutKind.threeBlocks;
  }

  final SeatLayoutKind kind;
  final Set<int> aisleAfterSeatNumbers;

  bool hasAisleAfter(int seatNumber) =>
      aisleAfterSeatNumbers.contains(seatNumber);
}

class SeatGridRow {
  const SeatGridRow({required this.label, required this.seatsByNumber});

  final String label;
  final Map<int, Seat> seatsByNumber;
}

/// Lưới ghế dựng từ `row_label` (hàng) và `seat_number` (cột).
///
/// Số ghế bị thiếu trong một hàng được để trống thay vì dồn ghế sang trái.
class SeatGrid {
  const SeatGrid({required this.rows, required this.columnCount});

  factory SeatGrid.fromSeats(List<Seat> seats) {
    final byRow = <String, Map<int, Seat>>{};
    var columnCount = 0;
    for (final seat in seats) {
      byRow.putIfAbsent(seat.rowLabel, () => {})[seat.seatNumber] = seat;
      if (seat.seatNumber > columnCount) columnCount = seat.seatNumber;
    }
    final labels = byRow.keys.toList()..sort(_compareRowLabels);
    return SeatGrid(
      rows: [
        for (final label in labels)
          SeatGridRow(label: label, seatsByNumber: byRow[label]!),
      ],
      columnCount: columnCount,
    );
  }

  final List<SeatGridRow> rows;
  final int columnCount;

  bool get isEmpty => rows.isEmpty;

  /// A…Z rồi AA…: so độ dài trước để hàng "AA" không chen giữa "A" và "B".
  static int _compareRowLabels(String a, String b) {
    final byLength = a.length.compareTo(b.length);
    return byLength != 0 ? byLength : a.compareTo(b);
  }
}
