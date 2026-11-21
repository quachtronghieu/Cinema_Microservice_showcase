import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_widgets.dart';
import '../view_models/watchlist_view_model.dart';

/// Nút lưu/bỏ phim khỏi watchlist, nền tối tròn để đọc được trên poster.
class WatchlistButton extends StatelessWidget {
  const WatchlistButton({super.key, required this.movieId, this.size = 40});

  final String movieId;
  final double size;

  Future<void> _onPressed(BuildContext context) async {
    final viewModel = context.read<WatchlistViewModel>();
    final router = GoRouter.of(context);

    final outcome = await viewModel.toggle(movieId);
    if (!context.mounted) return;

    switch (outcome) {
      case WatchlistToggleOutcome.added:
        showCinemaMessage(context, 'Đã thêm vào watchlist.');
      case WatchlistToggleOutcome.removed:
        showCinemaMessage(context, 'Đã bỏ khỏi watchlist.');
      case WatchlistToggleOutcome.signInRequired:
        showCinemaMessage(
          context,
          'Đăng nhập để lưu phim vào watchlist.',
          isError: true,
        );
        router.push('/login');
      case WatchlistToggleOutcome.failed:
        showCinemaMessage(
          context,
          viewModel.errorMessage ?? 'Không thể cập nhật watchlist.',
          isError: true,
        );
      case WatchlistToggleOutcome.ignored:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<WatchlistViewModel>();
    final saved = viewModel.contains(movieId);

    return Material(
      color: Colors.black.withValues(alpha: 0.55),
      shape: const CircleBorder(),
      child: IconButton(
        tooltip: saved ? 'Bỏ khỏi watchlist' : 'Thêm vào watchlist',
        onPressed: viewModel.isPending(movieId)
            ? null
            : () => _onPressed(context),
        iconSize: size * 0.5,
        constraints: BoxConstraints.tightFor(width: size, height: size),
        padding: EdgeInsets.zero,
        icon: Icon(
          saved ? Icons.bookmark : Icons.bookmark_border,
          color: Colors.white,
        ),
      ),
    );
  }
}
