import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';

/// Ánh xạ response của `POST /api/v1/customer/seat-holds`.
class SeatHoldDto {
  const SeatHoldDto({
    required this.holdId,
    required this.showtimeId,
    required this.seatIds,
    required this.expiresAt,
    required this.serverTime,
  });

  final String holdId;
  final String showtimeId;
  final List<String> seatIds;
  final String expiresAt;
  final String serverTime;

  factory SeatHoldDto.fromJson(Map<String, dynamic> json) {
    final seatIds = json['seatIds'];
    if (seatIds is! List) {
      throw const FormatException('Expected a list of seatIds.');
    }
    return SeatHoldDto(
      holdId: json['holdId'] as String,
      showtimeId: json['showtimeId'] as String,
      seatIds: seatIds.cast<String>().toList(growable: false),
      expiresAt: json['expiresAt'] as String,
      serverTime: json['serverTime'] as String,
    );
  }

  /// [receivedAt] là giờ máy lúc nhận response, dùng để tính
  /// [SeatHold.deadline].
  SeatHold toEntity({required DateTime receivedAt}) {
    final expires = _parseInstant(expiresAt);
    final server = _parseInstant(serverTime);
    return SeatHold(
      holdId: holdId,
      showtimeId: showtimeId,
      seatIds: seatIds,
      expiresAt: expires,
      serverTime: server,
      deadline: receivedAt.add(expires.difference(server)),
    );
  }

  /// Server gửi ISO-8601 có offset nên giá trị là thời điểm tuyệt đối.
  static DateTime _parseInstant(String value) {
    final parsed = DateTime.tryParse(value);
    if (parsed == null) throw FormatException('Invalid timestamp: $value');
    return parsed;
  }
}
