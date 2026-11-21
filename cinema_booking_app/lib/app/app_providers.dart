import 'package:flutter/widgets.dart';
import 'package:provider/provider.dart';

import '../core/network/api_client.dart';
import '../features/account/presentation/view_models/account_view_model.dart';
import '../features/bookings_and_payment/presentation/view_models/booking_summary_view_model.dart';
import '../features/customer/data/repositories/concession_api_repository.dart';
import '../features/customer/data/services/concession_api_service.dart';
import '../features/customer/domain/repositories/concession_repository.dart';
import '../features/customer/presentation/view_models/concession_view_model.dart';
import '../features/movies/data/repositories/movie_repository_impl.dart';
import '../features/movies/data/services/movie_api_service.dart';
import '../features/movies/data/services/trailer_launcher.dart';
import '../features/movies/domain/repositories/movie_repository.dart';
import '../features/movies/presentation/view_models/movie_discovery_view_model.dart';

import '../features/watchlist/data/repositories/watchlist_repository_impl.dart';
import '../features/watchlist/data/services/watchlist_api_service.dart';
import '../features/watchlist/domain/repositories/watchlist_repository.dart';
import '../features/watchlist/presentation/view_models/watchlist_view_model.dart';

import '../features/showtime_and_seat/domain/repositories/showtime_repository.dart';
import '../features/showtime_and_seat/data/repositories/showtime_repository_impl.dart';
import '../features/showtime_and_seat/data/services/showtime_api_service.dart';
import '../features/showtime_and_seat/data/repositories/seat_map_repository_impl.dart';
import '../features/showtime_and_seat/data/services/seat_map_api_service.dart';
import '../features/showtime_and_seat/domain/repositories/seat_map_repository.dart';
import '../features/showtime_and_seat/data/repositories/seat_hold_repository_impl.dart';
import '../features/showtime_and_seat/data/services/seat_hold_api_service.dart';
import '../features/showtime_and_seat/domain/repositories/seat_hold_repository.dart';

class AppProviders extends StatelessWidget {
  const AppProviders({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => MultiProvider(
    providers: [
      Provider<ApiClient>.value(value: ApiClient.instance),
      ChangeNotifierProvider<AccountViewModel>(
        create: (_) => AccountViewModel()..checkSession(),
      ),
      ChangeNotifierProvider<BookingSummaryViewModel>(
        create: (_) => BookingSummaryViewModel(),
      ),
      Provider<SeatMapRepository>(
        create: (context) => SeatMapRepositoryImpl(
          SeatMapApiService(context.read<ApiClient>().dio),
        ),
      ),
      Provider<SeatHoldRepository>(
        create: (context) => SeatHoldRepositoryImpl(
          SeatHoldApiService(context.read<ApiClient>().dio),
        ),
      ),
      Provider<TrailerLauncher>(
        create: (_) => const UrlLauncherTrailerLauncher(),
      ),
      Provider<MovieRepository>(
        create: (_) => MovieRepositoryImpl(MovieApiService()),
      ),
      Provider<WatchlistRepository>(
        create: (context) => WatchlistRepositoryImpl(
          WatchlistApiService(dio: context.read<ApiClient>().dio),
        ),
      ),
      ChangeNotifierProxyProvider<AccountViewModel, WatchlistViewModel>(
        create: (context) =>
            WatchlistViewModel(context.read<WatchlistRepository>()),
        update: (context, account, previous) {
          final session = account.userSession;
          return previous!..syncSession(
            account.loggedIn && session != null
                ? '${session.userId ?? ''}:${session.username}'
                : null,
          );
        },
      ),
      Provider<ShowtimeRepository>(
        create: (context) => ShowtimeRepositoryImpl(
          ShowtimeApiService(dio: context.read<ApiClient>().dio),
        ),
      ),
      ChangeNotifierProvider<MovieDiscoveryViewModel>(
        create: (context) =>
            MovieDiscoveryViewModel(context.read<MovieRepository>())..load(),
      ),
      Provider<ConcessionRepository>(
        create: (context) {
          final client = context.read<ApiClient>();

          return ConcessionApiRepository(
            service: ConcessionApiService(client),
            backendBaseUrl: Uri.parse(client.dio.options.baseUrl),
          );
        },
      ),
      ChangeNotifierProxyProvider<AccountViewModel, ConcessionsViewModel>(
        create: (context) => ConcessionsViewModel(
          context.read<ConcessionRepository>(),
          seatMapRepository: context.read<SeatMapRepository>(),
        ),
        update: (context, account, previous) {
          final catalog = previous!;
          final session = account.userSession;
          final sessionKey = account.loggedIn && session != null
              ? '${session.userId ?? ''}:${session.username}'
              : null;

          catalog.syncSession(sessionKey);
          return catalog;
        },
      ),
    ],
    child: child,
  );
}
