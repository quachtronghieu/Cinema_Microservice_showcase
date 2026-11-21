import 'package:flutter/foundation.dart';

class ApiConfig {
  ApiConfig._();

  static String _baseUrl = _defaultBaseUrl();

  static String _defaultBaseUrl() {
    const fromEnvironment = String.fromEnvironment('API_BASE_URL');
    if (fromEnvironment.isNotEmpty) {
      return fromEnvironment.replaceFirst(RegExp(r'/$'), '');
    }
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static String get baseUrl => _baseUrl;

  static void configure({String? baseUrl}) {
    final trimmedBaseUrl = baseUrl?.trim();
    if (trimmedBaseUrl != null && trimmedBaseUrl.isNotEmpty) {
      _baseUrl = trimmedBaseUrl.replaceAll(RegExp(r'/+$'), '');
    }
  }

  static void reset() {
    _baseUrl = _defaultBaseUrl();
  }

  static const login = '/api/auth/login';
  static const forgotPassword = '/api/auth/forgot-password';
  static const resetPassword = '/api/auth/reset-password';

  static const register = '/api/auth/register';
  static const logout = '/api/auth/logout';
  static const me = '/api/auth/me';
  static const customerBookings = '/api/v1/customer/bookings';
  static const customerBookingPreview = '$customerBookings/preview';
  static const customerTickets = '/api/v1/customer/tickets';
  static String customerDemoPayment(String bookingId) =>
      '$customerBookings/${Uri.encodeComponent(bookingId)}/demo-payment';
  static String customerTicket(String ticketId) =>
      '$customerTickets/${Uri.encodeComponent(ticketId)}';

  static const customerWatchlist = '/api/v1/customer/watchlist';
  static String customerWatchlistItem(String movieId) =>
      '$customerWatchlist/${Uri.encodeComponent(movieId)}';

  static const movies = '/api/v1/movies';
  static String movieDetail(String movieId) =>
      '$movies/${Uri.encodeComponent(movieId)}';

  /// Backend trả đường dẫn ảnh tương đối (ví dụ `/assets/img/movies/x.jpg`).
  static String? resolveAssetUrl(String? path) {
    final value = path?.trim() ?? '';
    if (value.isEmpty) return null;
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    return '$_baseUrl${value.startsWith('/') ? '' : '/'}$value';
  }

  //Ngan
  static const showtimes = '/api/v1/showtimes';

  static String showtimeSeats(String showtimeId) =>
      '$showtimes/${Uri.encodeComponent(showtimeId)}/seats';

  static String showtimeSeatAvailability(String showtimeId) =>
      '$showtimes/${Uri.encodeComponent(showtimeId)}/seat-availability';

  static const customerSeatHolds = '/api/v1/customer/seat-holds';

  static String customerSeatHold(String holdId) =>
      '$customerSeatHolds/${Uri.encodeComponent(holdId)}';
}
