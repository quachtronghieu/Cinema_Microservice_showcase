import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:cinema_booking_app/core/errors/failures.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_hold.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/seat_map.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/seat_hold_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/repositories/seat_map_repository.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/domain/use_cases/validate_seat_selection.dart';
import 'package:cinema_booking_app/features/showtime_and_seat/presentation/states/seat_selection_state.dart';

enum SeatToggleResult {
  selected,
  deselected,
  unavailable,
  limitReached,
  differentRow,
  notAdjacent,
}

/// Quản lý việc tải sơ đồ ghế, chọn/bỏ chọn ghế và giữ ghế khi tiếp tục.
///
/// Lựa chọn chỉ là nháp phía client cho tới khi [holdSelectedSeats] thành
/// công; server quyết định ghế còn trống và thời hạn giữ.
class SeatSelectionViewModel extends ChangeNotifier {
  SeatSelectionViewModel(
    this._repository,
    this._holdRepository, {
    this.maxSeats = defaultMaxSeats,
    DateTime Function()? now,
  }) : _now = now ?? DateTime.now;

  static const defaultMaxSeats = 8;

  final SeatMapRepository _repository;
  final SeatHoldRepository _holdRepository;
  final int maxSeats;
  final DateTime Function() _now;
  final _validateSelection = const ValidateSeatSelection();

  SeatSelectionState _state = const SeatSelectionState();
  SeatSelectionState get state => _state;

  int _requestNumber = 0;
  bool _disposed = false;
  Timer? _expiryTimer;

  Set<String> get _heldSeatIds =>
      _state.hold?.seatIds.toSet() ?? const <String>{};

  Future<void> load(String showtimeId) async {
    final requestNumber = ++_requestNumber;
    _emit(_state.copyWith(isLoading: true, clearError: true));

    final result = await _repository.getSeatMap(showtimeId);
    if (_disposed || requestNumber != _requestNumber) return;

    switch (result) {
      case Success<SeatMap>(:final data):
        // Ghế mình đang giữ là HELD trên server nhưng vẫn là lựa chọn của
        // mình; ghế khác vừa bị người khác giữ/đặt thì bỏ khỏi lựa chọn.
        final heldSeatIds = _heldSeatIds;
        final stillSelected = {
          for (final seat in data.seats)
            if (_state.selectedSeatIds.contains(seat.showtimeSeatId) &&
                (seat.isSelectable ||
                    heldSeatIds.contains(seat.showtimeSeatId)))
              seat.showtimeSeatId,
        };
        _emit(
          _state.copyWith(
            isLoading: false,
            clearError: true,
            seatMap: data,
            selectedSeatIds: stillSelected,
          ),
        );
      case FailureResult<SeatMap>(:final failure):
        _emit(_state.copyWith(isLoading: false, errorMessage: failure.message));
    }
  }

  SeatToggleResult toggle(Seat seat) {
    final selected = {..._state.selectedSeatIds};
    if (selected.remove(seat.showtimeSeatId)) {
      _emit(_state.copyWith(selectedSeatIds: selected));
      return SeatToggleResult.deselected;
    }
    // Ghế HELD do chính mình giữ (quay lại từ bước sau) vẫn chọn lại được.
    if (!seat.isSelectable && !_heldSeatIds.contains(seat.showtimeSeatId)) {
      return SeatToggleResult.unavailable;
    }
    if (selected.length >= maxSeats) return SeatToggleResult.limitReached;
    final violation = _validateSelection.checkAddition(
      seat: seat,
      selectedSeats: _state.selectedSeats,
    );
    if (violation != null) {
      return violation.type == SeatSelectionViolationType.differentRow
          ? SeatToggleResult.differentRow
          : SeatToggleResult.notAdjacent;
    }

    selected.add(seat.showtimeSeatId);
    _emit(_state.copyWith(selectedSeatIds: selected));
    return SeatToggleResult.selected;
  }

  void clearSelection() {
    if (_state.selectedSeatIds.isEmpty) return;
    _emit(_state.copyWith(selectedSeatIds: const {}));
  }

  /// Giữ các ghế đang chọn trước khi sang bước chọn bắp nước: server chuyển
  /// ghế sang HELD để tài khoản khác không chọn được. Nếu đã giữ đúng các
  /// ghế này và hold còn hạn thì dùng lại; chọn lại khác thì server thay hold
  /// cũ.
  ///
  /// Trả thông báo lỗi, hoặc `null` khi [SeatSelectionState.hold] đã sẵn sàng.
  Future<String?> holdSelectedSeats() async {
    final seatMap = _state.seatMap;
    final seatIds = [
      for (final seat in _state.selectedSeats) seat.showtimeSeatId,
    ];
    if (seatMap == null || seatIds.isEmpty || _state.isHolding) return null;
    final violation = _validateSelection(
      seats: seatMap.seats,
      selectedSeatIds: _state.selectedSeatIds,
      ownHeldSeatIds: _heldSeatIds,
    );
    if (violation != null) return _violationMessage(violation);

    final currentHold = _state.hold;
    if (currentHold != null &&
        !currentHold.isExpiredAt(_now()) &&
        setEquals(_heldSeatIds, seatIds.toSet())) {
      return null;
    }

    _emit(_state.copyWith(isHolding: true));
    final result = await _holdRepository.holdSeats(
      showtimeId: seatMap.showtimeId,
      seatIds: seatIds,
    );

    switch (result) {
      case Success<SeatHold>(:final data):
        if (_disposed) {
          // Màn hình đã đóng trong lúc chờ: nhả ngay để không giữ ghế vô ích.
          unawaited(_holdRepository.releaseHold(data.holdId));
          return null;
        }
        _emit(
          _state.copyWith(isHolding: false, hold: data, holdExpired: false),
        );
        _scheduleExpiry(data);
        return null;
      case FailureResult<SeatHold>(:final failure):
        _emit(_state.copyWith(isHolding: false));
        // Mọi xung đột đều có thể làm lệch sơ đồ đang hiển thị: tải lại để ghế
        // vừa bị người khác giữ hiện "Đang giữ" và rời lựa chọn.
        if (failure.statusCode == 409) unawaited(load(seatMap.showtimeId));
        final conflictSeatIds = _conflictSeatIds(failure);
        if (conflictSeatIds.isEmpty) return failure.message;
        final codes = [
          for (final seat in seatMap.seats)
            if (conflictSeatIds.contains(seat.showtimeSeatId)) seat.code,
        ];
        return 'Ghế ${codes.join(', ')} vừa được khách khác giữ hoặc đặt. '
            'Vui lòng chọn ghế khác.';
    }
  }

  Set<String> _conflictSeatIds(Failure failure) {
    final errors = failure.errors;
    final seatIds = errors?['seatIds'];
    if (errors?['code'] != 'SEAT_UNAVAILABLE' || seatIds is! List) {
      return const {};
    }
    return {for (final id in seatIds) id.toString()};
  }

  void _scheduleExpiry(SeatHold hold) {
    _expiryTimer?.cancel();
    _expiryTimer = Timer(
      hold.remainingAt(_now()),
      () => unawaited(_expireHold(hold)),
    );
  }

  /// Hết hạn giữ: bỏ hold khỏi UI ngay rồi tải lại để lựa chọn còn trống được
  /// giữ nguyên. Server tự nhả ghế theo `expiresAt` của nó nên đây chỉ là nhả
  /// sớm; lời gọi thất bại hoặc app bị tắt không làm ghế bị giữ mãi.
  Future<void> _expireHold(SeatHold hold) async {
    if (_disposed || _state.hold?.holdId != hold.holdId) return;
    _emit(_state.copyWith(clearHold: true, holdExpired: true));
    await _holdRepository.releaseHold(hold.holdId);
    if (_disposed) return;
    await load(hold.showtimeId);
  }

  String _violationMessage(SeatSelectionViolation violation) =>
      switch (violation.type) {
        SeatSelectionViolationType.differentRow =>
          'Chỉ được chọn các ghế trong cùng một hàng.',
        SeatSelectionViolationType.notContiguous =>
          'Các ghế đã chọn phải liền nhau.',
        SeatSelectionViolationType.leavesSingleGap =>
          'Không được để trống ghế lẻ ${violation.seatCodes.join(', ')}. '
              'Hãy chọn thêm ghế đó hoặc đổi vị trí.',
      };

  void _emit(SeatSelectionState state) {
    if (_disposed) return;
    _state = state;
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _expiryTimer?.cancel();
    // ViewModel gắn với route ghế: rời màn hình (back) là bỏ đặt, nên nhả hold
    // thay vì chờ server hết hạn. Các bước sau được push lên trên nên màn
    // hình này vẫn còn trong stack và hold được giữ.
    final hold = _state.hold;
    if (hold != null) unawaited(_holdRepository.releaseHold(hold.holdId));
    super.dispose();
  }
}
