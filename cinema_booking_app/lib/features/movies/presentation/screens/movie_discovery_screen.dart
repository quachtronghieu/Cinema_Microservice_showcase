import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../../watchlist/presentation/view_models/watchlist_view_model.dart';
import '../../../watchlist/presentation/widgets/watchlist_button.dart';
import '../../domain/entities/movie.dart';
import '../../domain/entities/movie_schedule.dart';
import '../states/movie_discovery_state.dart';
import '../view_models/movie_discovery_view_model.dart';
import '../widgets/movie_poster.dart';
import '../widgets/movie_rating_badge.dart';

class MovieDiscoveryScreen extends StatefulWidget {
  const MovieDiscoveryScreen({super.key});

  @override
  State<MovieDiscoveryScreen> createState() => _MovieDiscoveryScreenState();
}

class _MovieDiscoveryScreenState extends State<MovieDiscoveryScreen> {
  late final TextEditingController _searchController;

  /// Tab "MỤC YÊU THÍCH" chỉ là cách xem khác của UI, không phải lịch chiếu của server.
  bool _showSaved = false;

  void _selectSaved() {
    setState(() => _showSaved = true);
    context.read<WatchlistViewModel>().load();
  }

  void _selectSchedule(MovieSchedule schedule) {
    setState(() => _showSaved = false);
    context.read<MovieDiscoveryViewModel>().selectSchedule(schedule);
  }

  @override
  void initState() {
    super.initState();
    // ViewModel sống ở cấp app nên giữ lại từ khóa khi quay lại màn hình.
    _searchController = TextEditingController(
      text: context.read<MovieDiscoveryViewModel>().state.query,
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<MovieDiscoveryViewModel>();
    final state = vm.state;
    final nowShowing = state.schedule == MovieSchedule.nowShowing;

    return Scaffold(
      bottomNavigationBar: CinemaBottomNavigation(
        selectedIndex: 0,
        onItemSelected: (index) {
          if (index == 1) context.go('/tickets');
          if (index == 2) context.go('/account');
        },
      ),
      body: SafeArea(
        bottom: false,
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 390),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.fromLTRB(20, 18, 20, 12),
                  child: Row(children: [_MovieWordmark(), Spacer()]),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: TextField(
                    controller: _searchController,
                    onChanged: vm.updateQuery,
                    // Giới hạn độ dài `search` của API `/api/v1/movies`.
                    inputFormatters: [LengthLimitingTextInputFormatter(100)],
                    textInputAction: TextInputAction.search,
                    style: const TextStyle(fontSize: 14),
                    decoration: InputDecoration(
                      hintText: 'Tìm tên phim...',
                      hintStyle: const TextStyle(color: CinemaColors.muted),
                      prefixIcon: const Icon(Icons.search, size: 20),
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(vertical: 12),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(13),
                        borderSide: const BorderSide(color: Color(0xFF514444)),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(13),
                        borderSide: const BorderSide(color: CinemaColors.red),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 11),
                // Cuộn ngang để ba tab không tràn trên màn hẹp hoặc cỡ chữ lớn.
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Row(
                    children: [
                      _MovieScheduleTab(
                        label: 'ĐANG CHIẾU',
                        selected: nowShowing && !_showSaved,
                        onTap: () => _selectSchedule(MovieSchedule.nowShowing),
                      ),
                      const SizedBox(width: 16),
                      _MovieScheduleTab(
                        label: 'SẮP CHIẾU',
                        selected: !nowShowing && !_showSaved,
                        onTap: () => _selectSchedule(MovieSchedule.comingSoon),
                      ),
                      const SizedBox(width: 16),
                      _MovieScheduleTab(
                        label: 'MỤC YÊU THÍCH',
                        selected: _showSaved,
                        onTap: _selectSaved,
                      ),
                    ],
                  ),
                ),
                const Divider(height: 1, color: Color(0xFF2F2525)),
                if (_showSaved)
                  const SizedBox(height: 14)
                else ...[
                  const Padding(
                    padding: EdgeInsets.fromLTRB(20, 8, 20, 8),
                    child: Text(
                      'Phân loại theo ngày phát hành so với hôm nay.',
                      style: TextStyle(color: CinemaColors.muted, fontSize: 10),
                    ),
                  ),
                  _MovieCategoryChips(
                    categories: vm.categories,
                    selected: state.category,
                    onSelected: vm.selectCategory,
                  ),
                  const SizedBox(height: 7),
                ],
                Expanded(
                  child: _showSaved
                      ? _SavedMoviesBody(query: state.query)
                      : _MovieGridBody(vm: vm),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MovieGridBody extends StatelessWidget {
  const _MovieGridBody({required this.vm});

  final MovieDiscoveryViewModel vm;

  @override
  Widget build(BuildContext context) {
    final state = vm.state;
    final movies = vm.visibleMovies;
    final hasData = state.movies.isNotEmpty;

    // Khi đã có dữ liệu, refresh giữ nguyên lưới thay vì nháy về spinner.
    if (!hasData &&
        (state.status == MovieLoadStatus.loading ||
            state.status == MovieLoadStatus.initial)) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.status == MovieLoadStatus.failure && !hasData) {
      return AsyncStateView(
        message: state.errorMessage ?? 'Không tải được danh sách phim.',
        retry: vm.load,
        icon: Icons.error_outline,
      );
    }
    if (movies.isEmpty) {
      return RefreshIndicator(
        onRefresh: vm.load,
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            child: SizedBox(
              height: constraints.maxHeight,
              child: AsyncStateView(
                icon: Icons.movie_outlined,
                message: !hasData && state.query.trim().isEmpty
                    ? (state.schedule == MovieSchedule.nowShowing
                          ? 'Hiện chưa có phim đang chiếu.'
                          : 'Hiện chưa có phim sắp chiếu.')
                    : 'Không tìm thấy phim phù hợp.',
              ),
            ),
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: vm.load,
      child: _MovieGrid(movies: movies),
    );
  }
}

class _MovieGrid extends StatelessWidget {
  const _MovieGrid({required this.movies});

  final List<Movie> movies;

  @override
  Widget build(BuildContext context) => GridView.builder(
    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
    physics: const AlwaysScrollableScrollPhysics(),
    keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
    itemCount: movies.length,
    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
      crossAxisCount: 2,
      crossAxisSpacing: 12,
      mainAxisSpacing: 16,
      childAspectRatio: 0.57,
    ),
    itemBuilder: (context, index) => _MovieGridCard(movie: movies[index]),
  );
}

/// Phim khách đã lưu; dùng chung ô tìm kiếm với các tab còn lại.
class _SavedMoviesBody extends StatelessWidget {
  const _SavedMoviesBody({required this.query});

  final String query;

  @override
  Widget build(BuildContext context) {
    final watchlist = context.watch<WatchlistViewModel>();

    if (!watchlist.isSignedIn) {
      return Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const AsyncStateView(
            icon: Icons.bookmark_border,
            message: 'Đăng nhập để xem các phim đã lưu.',
          ),
          TextButton(
            onPressed: () => context.push('/login'),
            child: const Text('Đăng nhập'),
          ),
        ],
      );
    }

    final needle = query.trim().toLowerCase();
    final saved = watchlist.savedMovies;
    final movies = needle.isEmpty
        ? saved
        : saved
              .where((movie) => movie.title.toLowerCase().contains(needle))
              .toList(growable: false);

    if (saved.isEmpty &&
        (watchlist.status == WatchlistLoadStatus.initial ||
            watchlist.status == WatchlistLoadStatus.loading)) {
      return const Center(child: CircularProgressIndicator());
    }
    if (saved.isEmpty && watchlist.status == WatchlistLoadStatus.failure) {
      return AsyncStateView(
        message: watchlist.loadError ?? 'Không tải được watchlist.',
        retry: watchlist.load,
        icon: Icons.error_outline,
      );
    }
    if (movies.isEmpty) {
      return RefreshIndicator(
        onRefresh: watchlist.load,
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            child: SizedBox(
              height: constraints.maxHeight,
              child: AsyncStateView(
                icon: Icons.bookmark_border,
                message: saved.isEmpty
                    ? 'Bạn chưa lưu phim nào. Bấm biểu tượng bookmark trên phim để lưu.'
                    : 'Không tìm thấy phim phù hợp.',
              ),
            ),
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: watchlist.load,
      child: _MovieGrid(movies: movies),
    );
  }
}

class _MovieCategoryChips extends StatelessWidget {
  const _MovieCategoryChips({
    required this.categories,
    required this.selected,
    required this.onSelected,
  });

  final List<String> categories;

  /// `null` nghĩa là "Tất cả".
  final String? selected;
  final ValueChanged<String?> onSelected;

  @override
  Widget build(BuildContext context) {
    final options = <String?>[null, ...categories];
    return SizedBox(
      height: 42,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        scrollDirection: Axis.horizontal,
        itemCount: options.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final category = options[index];
          final isSelected = selected == category;
          return ChoiceChip(
            label: Text(category ?? 'Tất cả'),
            selected: isSelected,
            showCheckmark: false,
            onSelected: (_) => onSelected(category),
            labelStyle: TextStyle(
              color: isSelected ? Colors.white : CinemaColors.muted,
              fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
            ),
            selectedColor: CinemaColors.red,
            backgroundColor: CinemaColors.surface,
            side: BorderSide(
              color: isSelected ? CinemaColors.red : const Color(0xFF493D3D),
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
            ),
          );
        },
      ),
    );
  }
}

class _MovieWordmark extends StatelessWidget {
  const _MovieWordmark();

  @override
  Widget build(BuildContext context) => const Text.rich(
    TextSpan(
      children: [
        TextSpan(
          text: 'CINEMA',
          style: TextStyle(
            color: Colors.white,
            fontSize: 23,
            fontWeight: FontWeight.w900,
            letterSpacing: -1,
          ),
        ),
        TextSpan(
          text: '.',
          style: TextStyle(
            color: CinemaColors.red,
            fontSize: 25,
            fontWeight: FontWeight.w900,
          ),
        ),
      ],
    ),
  );
}

// class _MovieDemoBadge extends StatelessWidget {
//   const _MovieDemoBadge();

//   @override
//   Widget build(BuildContext context) => Container(
//     padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
//     decoration: BoxDecoration(
//       border: Border.all(color: CinemaColors.warning),
//       borderRadius: BorderRadius.circular(20),
//     ),
//     child: const Row(
//       mainAxisSize: MainAxisSize.min,
//       children: [
//         Icon(Icons.person_outline, color: CinemaColors.warning, size: 11),
//         SizedBox(width: 3),
//         Text(
//           'DEMO',
//           style: TextStyle(
//             color: CinemaColors.warning,
//             fontSize: 9,
//             fontWeight: FontWeight.w800,
//           ),
//         ),
//       ],
//     ),
//   );
// }

class _MovieScheduleTab extends StatelessWidget {
  const _MovieScheduleTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: selected ? CinemaColors.red : Colors.transparent,
            width: 2,
          ),
        ),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: selected ? Colors.white : CinemaColors.muted,
          fontSize: 12,
          fontWeight: FontWeight.w900,
        ),
      ),
    ),
  );
}

class _MovieGridCard extends StatelessWidget {
  const _MovieGridCard({required this.movie});

  final Movie movie;

  @override
  Widget build(BuildContext context) {
    final subtitle = [
      if (movie.genres.isNotEmpty) movie.genreLabel,
      if (movie.durationMinutes > 0) '${movie.durationMinutes}’',
    ].join(' · ');

    return InkWell(
      onTap: () => context.push('/movies/${Uri.encodeComponent(movie.id)}'),
      borderRadius: BorderRadius.circular(11),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Stack(
              children: [
                Positioned.fill(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(11),
                    child: MoviePoster(url: movie.posterUrl),
                  ),
                ),
                if (movie.ageRating.isNotEmpty)
                  Positioned(
                    left: 7,
                    top: 8,
                    child: MovieRatingBadge(rating: movie.ageRating),
                  ),
                Positioned(
                  right: 5,
                  top: 5,
                  child: WatchlistButton(movieId: movie.id, size: 32),
                ),
              ],
            ),
          ),
          const SizedBox(height: 7),
          Text(
            movie.title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 1),
          Text(
            subtitle,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(color: CinemaColors.muted, fontSize: 10),
          ),
        ],
      ),
    );
  }
}
