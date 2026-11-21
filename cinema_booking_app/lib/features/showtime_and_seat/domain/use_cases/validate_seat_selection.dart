import '../entities/seat_map.dart';

enum SeatSelectionViolationType {
  /// Các ghế chọn nằm ở nhiều hàng.
  differentRow,

  /// Các ghế chọn trong hàng không liền nhau.
  notContiguous,

  /// Lựa chọn để lại đúng một ghế trống kẹp giữa ghế đã đặt/giữ hoặc đầu hàng.
  leavesSingleGap,
}

class SeatSelectionViolation {
  const SeatSelectionViolation(this.type, {this.seatCodes = const []});

  final SeatSelectionViolationType type;

  /// Ghế lẻ bị bỏ trống (chỉ với [SeatSelectionViolationType.leavesSingleGap]).
  final List<String> seatCodes;
}

/// Luật chọn ghế chỉ áp dụng ở app (docs/ngan-task-scope.md, D-01): backend
/// không kiểm tra và không cần kiểm tra các luật này. Backend vẫn tự kiểm tra
/// 1–8 ghế, ghế còn trống và chủ hold.
///
/// - Chỉ chọn ghế trong cùng một hàng.
/// - Các ghế chọn phải liền nhau theo `seatNumber`.
/// - Không để lại đúng một ghế trống giữa lựa chọn và ghế đã đặt/đang giữ,
///   đầu hàng hoặc vị trí không có ghế. Cách từ 2 ghế trở lên thì hợp lệ.
///
/// Ghế do chính khách đang giữ ([ownHeldSeatIds]) được coi là trống nếu
/// không còn được chọn, vì hold sẽ được thay khi giữ lại.
class ValidateSeatSelection {
  const ValidateSeatSelection();

  /// Kiểm tra toàn bộ lựa chọn trước khi giữ ghế.
  SeatSelectionViolation? call({
    required List<Seat> seats,
    required Set<String> selectedSeatIds,
    Set<String> ownHeldSeatIds = const {},
  }) {
    final selected = [
      for (final seat in seats)
        if (selectedSeatIds.contains(seat.showtimeSeatId)) seat,
    ];
    if (selected.isEmpty) return null;

    final rowLabel = selected.first.rowLabel;
    if (selected.any((seat) => seat.rowLabel != rowLabel)) {
      return const SeatSelectionViolation(
        SeatSelectionViolationType.differentRow,
      );
    }

    final numbers = selected.map((seat) => seat.seatNumber).toList()..sort();
    for (var i = 1; i < numbers.length; i++) {
      if (numbers[i] != numbers[i - 1] + 1) {
        return const SeatSelectionViolation(
          SeatSelectionViolationType.notContiguous,
        );
      }
    }

    final row = {
      for (final seat in seats)
        if (seat.rowLabel == rowLabel) seat.seatNumber: seat,
    };
    bool isFree(int number) {
      final seat = row[number];
      if (seat == null || selectedSeatIds.contains(seat.showtimeSeatId)) {
        return false;
      }
      return seat.isSelectable || ownHeldSeatIds.contains(seat.showtimeSeatId);
    }

    // Lựa chọn đã liền nhau nên chỉ ghế sát hai đầu dãy có thể thành ghế lẻ.
    final singleGaps = [
      for (final (gap, outer) in [
        (numbers.first - 1, numbers.first - 2),
        (numbers.last + 1, numbers.last + 2),
      ])
        if (isFree(gap) && !isFree(outer)) row[gap]!.code,
    ];
    if (singleGaps.isNotEmpty) {
      return SeatSelectionViolation(
        SeatSelectionViolationType.leavesSingleGap,
        seatCodes: singleGaps,
      );
    }
    return null;
  }

  /// Kiểm tra nhanh khi chạm thêm [seat]: chỉ các luật không phụ thuộc thứ tự
  /// chọn. Luật ghế lẻ để tới lúc tiếp tục, vì dãy ghế đang chọn dở có thể
  /// tạm thời để lại ghế lẻ.
  SeatSelectionViolation? checkAddition({
    required Seat seat,
    required List<Seat> selectedSeats,
  }) {
    if (selectedSeats.isEmpty) return null;
    if (selectedSeats.any((other) => other.rowLabel != seat.rowLabel)) {
      return const SeatSelectionViolation(
        SeatSelectionViolationType.differentRow,
      );
    }
    final isAdjacent = selectedSeats.any(
      (other) => (other.seatNumber - seat.seatNumber).abs() == 1,
    );
    return isAdjacent
        ? null
        : const SeatSelectionViolation(
            SeatSelectionViolationType.notContiguous,
          );
  }
}
