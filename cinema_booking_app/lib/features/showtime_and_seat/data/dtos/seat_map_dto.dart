import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';

/// Ánh xạ sơ đồ vật lý từ cinema-service: `GET /api/v1/showtimes/{id}/seats`.
class SeatLayoutDto {
  const SeatLayoutDto({
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
  final String? showDate;
  final String? startTime;
  final List<LayoutSeatDto> seats;

  factory SeatLayoutDto.fromJson(Map<String, dynamic> json) => SeatLayoutDto(
    showtimeId: json['showtimeId'] as String,
    movieId: json['movieId'] as String?,
    movieTitle: json['movieTitle'] as String? ?? '',
    roomId: json['roomId'] as String?,
    templateId: json['templateId'] as String?,
    showDate: json['showDate'] as String?,
    startTime: json['startTime'] as String?,
    seats: _items(json['seats'], LayoutSeatDto.fromJson),
  );
}

class LayoutSeatDto {
  const LayoutSeatDto({
    required this.templateSeatId,
    required this.rowLabel,
    required this.seatNumber,
    required this.seatType,
  });

  final String templateSeatId;
  final String rowLabel;
  final int seatNumber;
  final String seatType;

  factory LayoutSeatDto.fromJson(Map<String, dynamic> json) => LayoutSeatDto(
    templateSeatId: json['templateSeatId'] as String,
    rowLabel: json['rowLabel'] as String,
    seatNumber: (json['seatNumber'] as num).toInt(),
    seatType: json['seatType'] as String? ?? '',
  );
}

/// Ánh xạ inventory thực từ booking-service:
/// `GET /api/v1/showtimes/{id}/seat-availability`.
class SeatAvailabilityDto {
  const SeatAvailabilityDto({required this.showtimeId, required this.seats});

  final String showtimeId;
  final List<AvailabilitySeatDto> seats;

  factory SeatAvailabilityDto.fromJson(Map<String, dynamic> json) =>
      SeatAvailabilityDto(
        showtimeId: json['showtimeId'] as String,
        seats: _items(json['seats'], AvailabilitySeatDto.fromJson),
      );
}

class AvailabilitySeatDto {
  const AvailabilitySeatDto({
    required this.showtimeSeatId,
    required this.templateSeatId,
    required this.status,
    this.unitPrice,
  });

  final String showtimeSeatId;
  final String templateSeatId;
  final String status;
  final num? unitPrice;

  factory AvailabilitySeatDto.fromJson(Map<String, dynamic> json) =>
      AvailabilitySeatDto(
        showtimeSeatId: json['showtimeSeatId'] as String,
        templateSeatId: json['templateSeatId'] as String,
        status: json['status'] as String? ?? '',
        unitPrice: json['unitPrice'] as num?,
      );
}

/// Ghép sơ đồ của Cinema với trạng thái của Booking theo `templateSeatId`.
///
/// Ghế vật lý chưa có inventory được giữ trên sơ đồ với trạng thái `unknown`
/// để không chọn được, không bao giờ bị coi là còn trống.
SeatMap mergeSeatMap(SeatLayoutDto layout, SeatAvailabilityDto availability) {
  if (layout.showtimeId != availability.showtimeId) {
    throw const FormatException('Seat map and availability do not match.');
  }
  final byTemplateSeat = {
    for (final seat in availability.seats) seat.templateSeatId: seat,
  };
  return SeatMap(
    showtimeId: layout.showtimeId,
    movieId: layout.movieId,
    movieTitle: layout.movieTitle,
    roomId: layout.roomId,
    templateId: layout.templateId,
    showDate: layout.showDate == null
        ? null
        : DateTime.tryParse(layout.showDate!),
    startTime: layout.startTime,
    seats: [
      for (final seat in layout.seats)
        _mergeSeat(seat, byTemplateSeat[seat.templateSeatId]),
    ],
  );
}

Seat _mergeSeat(LayoutSeatDto seat, AvailabilitySeatDto? inventory) => Seat(
  showtimeSeatId: inventory?.showtimeSeatId ?? '',
  templateSeatId: seat.templateSeatId,
  rowLabel: seat.rowLabel,
  seatNumber: seat.seatNumber,
  code: '${seat.rowLabel}${seat.seatNumber}',
  type: switch (seat.seatType.toUpperCase()) {
    'STANDARD' => SeatType.standard,
    'VIP' => SeatType.vip,
    _ => SeatType.unknown,
  },
  status: switch (inventory?.status.toUpperCase()) {
    'AVAILABLE' => SeatStatus.available,
    'HELD' => SeatStatus.pending,
    'SOLD' => SeatStatus.unavailable,
    _ => SeatStatus.unknown,
  },
  price: inventory?.unitPrice,
);

List<T> _items<T>(Object? value, T Function(Map<String, dynamic>) fromJson) {
  if (value is! List) {
    throw const FormatException('Expected a list of seats.');
  }
  return value
      .map((item) {
        if (item is! Map) throw const FormatException('Invalid seat item.');
        return fromJson(Map<String, dynamic>.from(item));
      })
      .toList(growable: false);
}
