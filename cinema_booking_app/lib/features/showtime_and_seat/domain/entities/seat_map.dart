enum SeatType { standard, vip, unknown }

/// Trạng thái ghế của một suất chiếu do server quyết định.
enum SeatStatus {
  available,

  /// `HELD`: đang được giữ tạm, chưa thanh toán.
  pending,

  /// `SOLD`: đã bán.
  unavailable,

  /// Giá trị lạ hoặc ghế chưa có inventory bên Booking.
  unknown,
}

class Seat {
  const Seat({
    required this.showtimeSeatId,
    required this.templateSeatId,
    required this.rowLabel,
    required this.seatNumber,
    required this.code,
    required this.type,
    required this.status,
    this.price,
  });

  /// ID ghế theo suất do Booking cấp, dùng cho hold/booking. Rỗng khi ghế
  /// chưa có inventory; khi đó ghế không chọn được.
  final String showtimeSeatId;
  final String templateSeatId;
  final String rowLabel;
  final int seatNumber;
  final String code;
  final SeatType type;
  final SeatStatus status;

  /// Giá chỉ để hiển thị; tiền chính thức luôn do backend báo giá.
  final num? price;

  /// Trạng thái lạ được coi là không khả dụng để không cho chọn nhầm.
  bool get isSelectable => status == SeatStatus.available;
}

class SeatMap {
  const SeatMap({
    required this.showtimeId,
    required this.movieTitle,
    required this.seats,
    this.movieId,
    this.roomId,
    this.templateId,
    this.showDate,
    this.startTime,
  });

  final String showtimeId;
  final String? movieId;
  final String movieTitle;
  final String? roomId;
  final String? templateId;
  final DateTime? showDate;
  final String? startTime;
  final List<Seat> seats;
}
