/// Lượt giữ ghế do server tạo. Đây là dữ liệu bàn giao cho các bước sau
/// (contract TV3 → TV4/TV5: `holdId`, `showtimeId`, `seatIds`, `expiresAt`,
/// `serverTime`).
class SeatHold {
  const SeatHold({
    required this.holdId,
    required this.showtimeId,
    required this.seatIds,
    required this.expiresAt,
    required this.serverTime,
    required this.deadline,
  });

  final String holdId;
  final String showtimeId;

  /// `showtimeSeatId` của các ghế đang giữ.
  final List<String> seatIds;

  /// Hạn giữ theo đồng hồ server; so với [serverTime], không so với giờ máy.
  final DateTime expiresAt;
  final DateTime serverTime;

  /// Mốc hết hạn quy đổi sang đồng hồ máy lúc nhận response
  /// (`giờ máy + (expiresAt - serverTime)`), nên không bị lệch nếu giờ máy
  /// sai. Dùng mốc này cho đếm ngược và hẹn giờ.
  final DateTime deadline;

  Duration remainingAt(DateTime now) {
    final remaining = deadline.difference(now);
    return remaining.isNegative ? Duration.zero : remaining;
  }

  bool isExpiredAt(DateTime now) => !deadline.isAfter(now);
}
