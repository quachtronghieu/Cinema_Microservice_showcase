import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../core/network/api_client.dart';

import '../features/customer/presentation/screens/concession_catalog_screen.dart';
import '../features/customer/presentation/screens/concession_cart_screen.dart';
import '../core/ui/cinema_widgets.dart';
import '../features/account/presentation/screens/account_screen.dart';
import '../features/account/presentation/screens/login_registration_screen.dart';
import '../features/account/presentation/screens/reset_password_screen.dart';
import '../features/bookings_and_payment/domain/entities/booking_summary.dart';
import '../features/bookings_and_payment/presentation/screens/booking_created_screen.dart';
import '../features/bookings_and_payment/presentation/screens/customer_ticket_detail_screen.dart';
import '../features/bookings_and_payment/presentation/screens/demo_payment_screen.dart';
import '../features/bookings_and_payment/presentation/screens/my_tickets_screen.dart';
import '../features/bookings_and_payment/presentation/screens/booking_summary_debug_screen.dart';
import '../features/bookings_and_payment/presentation/screens/booking_summary_screen.dart';
import '../features/bookings_and_payment/presentation/view_models/demo_payment_view_model.dart';
import '../features/bookings_and_payment/presentation/view_models/customer_tickets_view_model.dart';
import '../features/movies/presentation/screens/movie_detail_screen.dart';
import '../features/movies/presentation/screens/movie_discovery_screen.dart';

import '../features/movies/domain/repositories/movie_repository.dart';
import '../features/showtime_and_seat/domain/repositories/showtime_repository.dart';
import '../features/showtime_and_seat/presentation/screens/showtime_screen.dart';
import '../features/showtime_and_seat/domain/repositories/seat_hold_repository.dart';
import '../features/showtime_and_seat/presentation/view_models/showtime_view_model.dart';
import '../features/showtime_and_seat/domain/repositories/seat_map_repository.dart';
import '../features/showtime_and_seat/presentation/screens/seat_selection_screen.dart';
import '../features/showtime_and_seat/presentation/view_models/seat_selection_view_model.dart';
import '../features/showtime_and_seat/domain/entities/seat_hold.dart';

final appRouter = GoRouter(
  initialLocation: '/login',
  refreshListenable: ApiClient.sessionExpired,
  redirect: (context, state) {
    if (ApiClient.sessionExpired.value &&
        state.uri.path != '/login') {
      return '/login';
    }

    return null;
  },
  routes: [
    GoRoute(
      path: '/concessions',
      builder: (context, state) {
        final extra = state.extra;

        return ConcessionCatalogScreen(hold: extra is SeatHold ? extra : null);
      },
    ),
    GoRoute(
      path: '/cart',
      builder: (context, state) {
        final extra = state.extra;

        return ConcessionCartScreen(hold: extra is SeatHold ? extra : null);
      },
    ),
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginRegistrationScreen(),
    ),
    GoRoute(
      path: '/movies',
      builder: (context, state) => const MovieDiscoveryScreen(),
      routes: [
        GoRoute(
          path: ':movieId',
          builder: (context, state) =>
              MovieDetailScreen(movieId: state.pathParameters['movieId']!),
        ),
      ],
    ),
    GoRoute(
      path: '/account',
      builder: (context, state) => const AccountScreen(),
    ),
    GoRoute(
      path: '/reset-password/:id/:timestamp/:token',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        final timestamp =
            int.tryParse(state.pathParameters['timestamp'] ?? '') ?? 0;
        final token = state.pathParameters['token'] ?? '';
        return ResetPasswordScreen(
          id: id,
          resetTimestamp: timestamp,
          resetCode: token,
        );
      },
    ),

    GoRoute(
      path: '/showtimes/:movieId',
      builder: (context, state) {
        final movieId = state.pathParameters['movieId']!;
        final extra = state.extra;
        // ViewModel theo route để suất chiếu của phim trước không hiện lại.
        return ChangeNotifierProvider<ShowtimeViewModel>(
          create: (context) =>
              ShowtimeViewModel(
                  context.read<ShowtimeRepository>(),
                  context.read<MovieRepository>(),
                )
                ..loadMovie(movieId)
                ..load(movieId, DateUtils.dateOnly(DateTime.now())),
          child: ShowtimeSelectionScreen(
            movieId: movieId,
            movieTitle: extra is String ? extra : null,
          ),
        );
      },
    ),
    GoRoute(
      path: '/seats/:showtimeId',
      builder: (context, state) {
        final showtimeId = state.pathParameters['showtimeId']!;
        return ChangeNotifierProvider<SeatSelectionViewModel>(
          create: (context) => SeatSelectionViewModel(
            context.read<SeatMapRepository>(),
            context.read<SeatHoldRepository>(),
          )..load(showtimeId),
          child: SeatSelectionScreen(showtimeId: showtimeId),
        );
      },
    ),
    GoRoute(
      path: '/tickets',
      builder: (context, state) =>
          ChangeNotifierProvider<CustomerTicketsViewModel>(
            create: (_) => CustomerTicketsViewModel()..loadTickets(),
            child: const MyTicketsScreen(),
          ),
      routes: [
        GoRoute(
          path: ':ticketId',
          builder: (context, state) {
            final ticketId = state.pathParameters['ticketId']!;
            return ChangeNotifierProvider<CustomerTicketsViewModel>(
              create: (_) => CustomerTicketsViewModel()..loadTicket(ticketId),
              child: CustomerTicketDetailScreen(ticketId: ticketId),
            );
          },
        ),
      ],
    ),
    GoRoute(
      path: '/booking-summary',
      builder: (context, state) {
        final selection = state.extra;
        if (selection is BookingSelection) {
          return BookingSummaryScreen(selection: selection);
        }
        return const _FeaturePlaceholder(title: 'Booking summary');
      },
    ),
    GoRoute(
      path: '/booking-summary-debug',
      builder: (context, state) => const BookingSummaryDebugScreen(),
    ),
    GoRoute(
      path: '/booking-created',
      builder: (context, state) {
        final booking = state.extra;
        if (booking is CreatedBooking) {
          return BookingCreatedScreen(booking: booking);
        }
        return const _FeaturePlaceholder(title: 'Đặt vé');
      },
    ),
    GoRoute(
      path: '/demo-payment',
      builder: (context, state) {
        final booking = state.extra;
        if (booking is CreatedBooking) {
          return ChangeNotifierProvider<DemoPaymentViewModel>(
            create: (_) => DemoPaymentViewModel(),
            child: DemoPaymentScreen(booking: booking),
          );
        }
        return const _FeaturePlaceholder(title: 'Thanh toán demo');
      },
    ),
  ],
);

class _FeaturePlaceholder extends StatelessWidget {
  const _FeaturePlaceholder({required this.title});
  final String title;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(title)),
    body: const AsyncStateView(
      icon: Icons.movie_outlined,
      message: 'Tính năng đang được phát triển.',
    ),
    bottomNavigationBar: CinemaBottomNavigation(
      selectedIndex: title == 'Movies' ? 0 : 1,
      onItemSelected: (idx) {
        if (idx == 0) {
          context.go('/movies');
        } else if (idx == 1) {
          context.go('/tickets');
        } else {
          context.go('/account');
        }
      },
    ),
  );
}
