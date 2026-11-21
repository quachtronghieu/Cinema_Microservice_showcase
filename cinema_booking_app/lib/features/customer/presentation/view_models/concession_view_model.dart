import 'package:flutter/foundation.dart';

import '../../../../core/errors/app_failure.dart';
import '../../domain/entities/cinema_models.dart';
import '../../domain/repositories/concession_repository.dart';
import '../../../../core/network/api_result.dart';
import '../../../showtime_and_seat/domain/entities/seat_hold.dart';
import '../../../showtime_and_seat/domain/entities/seat_map.dart';
import '../../../showtime_and_seat/domain/repositories/seat_map_repository.dart';

enum ConcessionFilter { all, popcorn, drink }

class ConcessionsViewModel extends ChangeNotifier {
  static const maxQuantityPerItem = 10;
  ConcessionsViewModel(this.repository, {this.seatMapRepository});
  final ConcessionRepository repository;
  final SeatMapRepository? seatMapRepository;

  int? ticketSubtotal;
  String? ticketError;
  bool ticketLoading = false;
  int _ticketRequest = 0;

  int? get previewTotal {
    final tickets = ticketSubtotal;
    return tickets == null ? null : tickets + foodSubtotal;
  }

  List<CinemaConcession> catalog = [];
  Map<String, int> _cart = {};
  Map<String, int> get cart => Map.unmodifiable(_cart);
  String? _cartHoldId;
  String? cartNotice;
  String? error;

  bool _loading = false;
  bool _disposed = false;
  int _loadRequest = 0;
  String? _sessionKey;
  bool _sessionInitialized = false;

  ConcessionFilter _filter = ConcessionFilter.all;

  ConcessionFilter get filter => _filter;

  List<CinemaConcession> get filteredCatalog {
    final selectedType = switch (_filter) {
      ConcessionFilter.all => null,
      ConcessionFilter.popcorn => 'PC',
      ConcessionFilter.drink => 'DR',
    };

    return List<CinemaConcession>.unmodifiable(
      selectedType == null
          ? catalog
          : catalog.where((item) => item.type == selectedType),
    );
  }

  void setFilter(ConcessionFilter value) {
    if (_disposed || _filter == value) {
      return;
    }

    _filter = value;
    notifyListeners();
  }

  void syncSession(String? sessionKey) {
    if (_disposed || (_sessionInitialized && _sessionKey == sessionKey)) {
      return;
    }

    _sessionInitialized = true;
    _sessionKey = sessionKey;

    Future<void>.microtask(() async {
      if (_disposed || _sessionKey != sessionKey) {
        return;
      }

      reset(clearCatalog: true);

      if (sessionKey == null) {
        error = 'Đăng nhập trước để tải danh mục bắp/nước.';
        notifyListeners();
        return;
      }

      await load();
    });
  }

  bool get busy => _loading;

  int get count => cart.values.fold(0, (sum, quantity) => sum + quantity);

  // Tạm tính hiển thị, chưa phải báo giá backend xác nhận.
  int get foodSubtotal =>
      catalog.fold(0, (sum, item) => sum + item.price * (cart[item.id] ?? 0));

  // A temporary cart follows its first hold. A different booking starts empty.
  void syncHold(String? holdId) {
    if (_disposed || _cartHoldId == holdId) return;
    if (_cartHoldId != null) {
      if (_cart.isNotEmpty) {
        cartNotice = 'Đã xóa giỏ bắp/nước của lượt giữ ghế trước.';
      }
      _cart = {};
    }
    _cartHoldId = holdId;
    _ticketRequest++;
    ticketSubtotal = null;
    ticketError = null;
    ticketLoading = false;
    notifyListeners();
  }

  void addItem(String id) {
    // Read at command time: multiple taps may arrive before the next UI build.
    setQuantity(id, (_cart[id] ?? 0) + 1);
  }

  void decreaseItem(String id) {
    setQuantity(id, (_cart[id] ?? 0) - 1);
  }

  void dismissCartNotice() {
    if (_disposed || cartNotice == null) return;
    cartNotice = null;
    notifyListeners();
  }

  Future<void> loadTicketPreview(SeatHold? hold) async {
    if (_disposed) {
      return;
    }

    final request = ++_ticketRequest;

    ticketSubtotal = null;
    ticketError = null;
    ticketLoading = false;

    if (hold == null) {
      notifyListeners();
      return;
    }

    if (hold.isExpiredAt(DateTime.now())) {
      ticketError = 'Thời gian giữ ghế đã hết. Vui lòng chọn ghế lại.';
      notifyListeners();
      return;
    }

    final source = seatMapRepository;
    if (source == null) {
      ticketError = 'Chưa cấu hình nguồn dữ liệu giá ghế.';
      notifyListeners();
      return;
    }

    ticketLoading = true;
    notifyListeners();

    try {
      final result = await source.getSeatMap(hold.showtimeId);

      if (_disposed || request != _ticketRequest) {
        return;
      }

      switch (result) {
        case Success<SeatMap>(:final data):
          final ids = hold.seatIds.toSet();
          final seats = data.seats
              .where((seat) => ids.contains(seat.showtimeSeatId))
              .toList();

          if (data.showtimeId != hold.showtimeId ||
              ids.isEmpty ||
              ids.length != hold.seatIds.length ||
              seats.length != ids.length) {
            ticketError = 'Không tìm đủ thông tin các ghế đã chọn.';
            return;
          }

          var subtotal = 0;

          for (final seat in seats) {
            final price = seat.price;

            if (price == null ||
                !price.isFinite ||
                price < 0 ||
                price != price.toInt()) {
              ticketError = 'Chưa có giá hợp lệ cho ghế ${seat.code}.';
              return;
            }

            subtotal += price.toInt();
          }

          ticketSubtotal = subtotal;

        case FailureResult<SeatMap>(:final failure):
          ticketError = failure.message;
      }
    } catch (_) {
      if (!_disposed && request == _ticketRequest) {
        ticketError = 'Không tải được giá ghế. Vui lòng thử lại.';
      }
    } finally {
      if (!_disposed && request == _ticketRequest) {
        ticketLoading = false;
        notifyListeners();
      }
    }
  }

  Future<void> load() async {
    if (_disposed) {
      return;
    }

    final request = ++_loadRequest;
    _loading = true;
    error = null;
    notifyListeners();

    try {
      final result = await repository.listConcessions();

      if (_disposed || request != _loadRequest) {
        return;
      }

      final availableIds = result.map((item) => item.id).toSet();
      final removedNames = catalog
          .where(
            (item) =>
                _cart.containsKey(item.id) && !availableIds.contains(item.id),
          )
          .map((item) => item.name)
          .toList();
      catalog = List.unmodifiable(result);
      _cart = {
        for (final entry in _cart.entries)
          if (availableIds.contains(entry.key)) entry.key: entry.value,
      };
      if (removedNames.isNotEmpty) {
        cartNotice =
            'Đã bỏ món không còn trong danh mục khỏi giỏ: ${removedNames.join(', ')}.';
      }
    } catch (e) {
      if (!_disposed && request == _loadRequest) {
        error = e is AppFailure
            ? e.message
            : 'Không tải được danh mục. Vui lòng thử lại.';
      }
    } finally {
      if (!_disposed && request == _loadRequest) {
        _loading = false;
        notifyListeners();
      }
    }
  }

  void setQuantity(String id, int quantity) {
    if (_disposed) {
      return;
    }

    if (quantity > 0 && _sessionInitialized && _sessionKey == null) return;

    if (quantity > 0 && !catalog.any((item) => item.id == id)) {
      error = 'Món này không có trong danh mục hiện tại.';
      notifyListeners();
      return;
    }

    final next = {..._cart};

    if (quantity <= 0) {
      next.remove(id);
    } else {
      next[id] = quantity.clamp(1, maxQuantityPerItem).toInt();
    }

    if (mapEquals(_cart, next)) return;
    _cart = next;
    notifyListeners();
  }

  void clearCart() {
    if (_disposed || cart.isEmpty) {
      return;
    }

    _cart = {};
    notifyListeners();
  }

  void reset({bool clearCatalog = false}) {
    if (_disposed) {
      return;
    }
    _ticketRequest++;
    ticketSubtotal = null;
    ticketError = null;
    ticketLoading = false;
    _loadRequest++;
    _loading = false;
    _cart = {};
    _cartHoldId = null;
    cartNotice = null;
    error = null;
    _filter = ConcessionFilter.all;

    if (clearCatalog) {
      catalog = [];
    }

    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _loadRequest++;
    _ticketRequest++;
    super.dispose();
  }
}
