import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';

class SeatSelectionState {
  const SeatSelectionState({
    this.isLoading = false,
    this.errorMessage,
    this.seatMap,
    this.selectedSeatIds = const {},
    this.hold,
    this.isHolding = false,
    this.holdExpired = false,
  });

  final bool isLoading;
  final String? errorMessage;
  final SeatMap? seatMap;

  /// `showtimeSeatId` của các ghế đang chọn trên UI, gồm cả ghế đang giữ.
  final Set<String> selectedSeatIds;

  /// Hold server trả về khi bấm "Tiếp tục"; `null` khi lựa chọn mới chỉ là
  /// nháp phía client.
  final SeatHold? hold;
  final bool isHolding;

  /// Hold gần nhất đã hết hạn và được nhả; hết cờ khi giữ ghế lại.
  final bool holdExpired;

  /// Ghế đã chọn, theo thứ tự xuất hiện trên sơ đồ.
  List<Seat> get selectedSeats => [
    for (final seat in seatMap?.seats ?? const <Seat>[])
      if (selectedSeatIds.contains(seat.showtimeSeatId)) seat,
  ];

  SeatSelectionState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    SeatMap? seatMap,
    Set<String>? selectedSeatIds,
    SeatHold? hold,
    bool clearHold = false,
    bool? isHolding,
    bool? holdExpired,
  }) => SeatSelectionState(
    isLoading: isLoading ?? this.isLoading,
    errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    seatMap: seatMap ?? this.seatMap,
    selectedSeatIds: selectedSeatIds ?? this.selectedSeatIds,
    hold: clearHold ? null : hold ?? this.hold,
    isHolding: isHolding ?? this.isHolding,
    holdExpired: holdExpired ?? this.holdExpired,
  );
}
