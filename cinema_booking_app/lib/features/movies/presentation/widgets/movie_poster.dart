import 'package:flutter/material.dart';

import '../../../../core/ui/cinema_theme.dart';

/// Poster tải từ backend; hiển thị khung thay thế khi thiếu ảnh hoặc lỗi tải.
class MoviePoster extends StatelessWidget {
  const MoviePoster({super.key, required this.url});

  final String? url;

  @override
  Widget build(BuildContext context) {
    final source = url;
    if (source == null) return const _PosterPlaceholder();
    return Image.network(
      source,
      fit: BoxFit.cover,
      errorBuilder: (_, _, _) => const _PosterPlaceholder(),
      loadingBuilder: (context, child, progress) =>
          progress == null ? child : const _PosterPlaceholder(),
    );
  }
}

class _PosterPlaceholder extends StatelessWidget {
  const _PosterPlaceholder();

  @override
  Widget build(BuildContext context) => const ColoredBox(
    color: CinemaColors.surface,
    child: Center(
      child: Icon(Icons.movie_outlined, color: CinemaColors.muted, size: 36),
    ),
  );
}
