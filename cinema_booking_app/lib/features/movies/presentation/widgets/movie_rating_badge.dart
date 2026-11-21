import 'package:flutter/material.dart';

import '../../../../core/ui/cinema_theme.dart';

class MovieRatingBadge extends StatelessWidget {
  const MovieRatingBadge({super.key, required this.rating});

  final String rating;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
    decoration: BoxDecoration(
      color: CinemaColors.red,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Text(
      rating,
      style: const TextStyle(
        color: Colors.white,
        fontSize: 10,
        fontWeight: FontWeight.w800,
      ),
    ),
  );
}
