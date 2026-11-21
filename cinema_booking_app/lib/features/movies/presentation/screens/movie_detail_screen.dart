import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../../watchlist/presentation/widgets/watchlist_button.dart';
import '../../domain/entities/movie.dart';
import '../../data/services/trailer_launcher.dart';
import '../../domain/repositories/movie_repository.dart';
import '../states/movie_discovery_state.dart';
import '../view_models/movie_detail_view_model.dart';
import '../widgets/movie_poster.dart';
import '../widgets/movie_rating_badge.dart';

final _releaseDateFormat = DateFormat('dd/MM/yyyy');

class MovieDetailScreen extends StatelessWidget {
  const MovieDetailScreen({super.key, required this.movieId});

  final String movieId;

  @override
  Widget build(BuildContext context) =>
      ChangeNotifierProvider<MovieDetailViewModel>(
        create: (context) => MovieDetailViewModel(
          context.read<MovieRepository>(),
          movieId: movieId,
        )..load(),
        child: const _MovieDetailView(),
      );
}

class _MovieDetailView extends StatelessWidget {
  const _MovieDetailView();

  void _goBack(BuildContext context) =>
      context.canPop() ? context.pop() : context.go('/movies');

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<MovieDetailViewModel>();
    final state = vm.state;
    final movie = state.movie;

    if (movie == null) {
      return Scaffold(
        appBar: AppBar(
          leading: IconButton(
            onPressed: () => _goBack(context),
            icon: const Icon(Icons.chevron_left),
          ),
        ),
        body: state.status == MovieLoadStatus.failure
            ? AsyncStateView(
                message: state.errorMessage ?? 'Chưa tải được chi tiết phim.',
                retry: state.notFound ? null : vm.load,
                icon: state.notFound
                    ? Icons.movie_outlined
                    : Icons.error_outline,
              )
            : const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        leadingWidth: 64,
        leading: Padding(
          padding: const EdgeInsets.only(left: 16, top: 7, bottom: 7),
          child: Material(
            color: Colors.black.withValues(alpha: 0.55),
            shape: const CircleBorder(),
            child: IconButton(
              tooltip: 'Quay lại',
              onPressed: () => _goBack(context),
              icon: const Icon(Icons.chevron_left),
            ),
          ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16, top: 7, bottom: 7),
            child: WatchlistButton(movieId: movie.id, size: 42),
          ),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 390),
          child: RefreshIndicator(
            onRefresh: vm.load,
            child: ListView(
              padding: EdgeInsets.zero,
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                SizedBox(
                  height: 290,
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      MoviePoster(url: movie.posterUrl),
                      const DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              Colors.transparent,
                              Color(0x330E0B0B),
                              CinemaColors.background,
                            ],
                            stops: [0.45, 0.72, 1],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                  child: _MovieDetailContent(movie: movie),
                ),
              ],
            ),
          ),
        ),
      ),
      bottomNavigationBar: DecoratedBox(
        decoration: const BoxDecoration(
          color: Color(0xFF171111),
          border: Border(top: BorderSide(color: Color(0xFF332929))),
        ),
        child: SafeArea(
          top: false,
          child: Center(
            heightFactor: 1,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 390),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 11, 16, 13),
                child: ElevatedButton.icon(
                  // Hợp đồng TV2 → TV3: chỉ truyền movieId.
                  onPressed: () => context.push(
                    '/showtimes/${Uri.encodeComponent(movie.id)}',
                  ),
                  icon: const Icon(Icons.confirmation_num_outlined, size: 17),
                  label: const Text('Chọn suất chiếu'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: CinemaColors.red,
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(48),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(13),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MovieDetailContent extends StatelessWidget {
  const _MovieDetailContent({required this.movie});

  final Movie movie;

  Future<void> _openTrailer(BuildContext context, String url) async {
    final opened = await context.read<TrailerLauncher>().open(url);
    if (!opened && context.mounted) {
      showCinemaMessage(
        context,
        'Không mở được trailer. Vui lòng thử lại sau.',
        isError: true,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final releaseDate = movie.releaseDate;
    final trailerUrl = movie.trailerUrl;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (movie.ageRating.isNotEmpty) ...[
          MovieRatingBadge(rating: movie.ageRating),
          const SizedBox(height: 8),
        ],
        Text(
          movie.title.toUpperCase(),
          style: const TextStyle(
            fontSize: 28,
            height: 1,
            fontWeight: FontWeight.w900,
            letterSpacing: -1,
          ),
        ),
        const SizedBox(height: 7),
        Wrap(
          spacing: 12,
          runSpacing: 4,
          children: [
            if (movie.durationMinutes > 0)
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.schedule,
                    size: 15,
                    color: CinemaColors.muted,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '${movie.durationMinutes} phút',
                    style: const TextStyle(
                      color: CinemaColors.muted,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            if (movie.genres.isNotEmpty)
              Text(
                movie.genreLabel,
                style: const TextStyle(color: CinemaColors.muted, fontSize: 13),
              ),
          ],
        ),
        const SizedBox(height: 18),
        OutlinedButton.icon(
          onPressed: trailerUrl == null
              ? null
              : () => _openTrailer(context, trailerUrl),
          icon: const Icon(Icons.play_circle_outline, size: 18),
          label: Text(trailerUrl == null ? 'Chưa có trailer' : 'Xem trailer'),
          style: OutlinedButton.styleFrom(
            foregroundColor: CinemaColors.text,
            backgroundColor: CinemaColors.surface,
            minimumSize: const Size.fromHeight(48),
            side: const BorderSide(color: Color(0xFF594B4B)),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(13),
            ),
          ),
        ),
        const SizedBox(height: 27),
        const Text(
          'NỘI DUNG',
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 10),
        Text(
          movie.summary.isNotEmpty
              ? movie.summary
              : 'Nội dung phim đang được cập nhật.',
          style: const TextStyle(
            color: Color(0xFFD1C6C6),
            fontSize: 13,
            height: 1.55,
          ),
        ),
        const SizedBox(height: 18),
        Container(
          decoration: BoxDecoration(
            color: const Color(0xFF1B1515),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            children: [
              _MovieInformationRow(
                label: 'Ngày phát hành',
                value: releaseDate == null
                    ? '—'
                    : _releaseDateFormat.format(releaseDate),
              ),
              const Divider(height: 1, color: Color(0xFF372D2D)),
              _MovieInformationRow(
                label: 'Phân loại',
                value: movie.ageRating.isNotEmpty ? movie.ageRating : '—',
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _MovieInformationRow extends StatelessWidget {
  const _MovieInformationRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
    child: Row(
      children: [
        Text(label, style: const TextStyle(color: CinemaColors.muted)),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    ),
  );
}
