import 'package:flutter/material.dart';

import 'cinema_theme.dart';

void showCinemaMessage(
  BuildContext context,
  String message, {
  bool isError = false,
}) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? CinemaColors.red : CinemaColors.surface,
        behavior: SnackBarBehavior.floating,
      ),
    );
}

class AsyncStateView extends StatelessWidget {
  const AsyncStateView({
    super.key,
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.retry,
  });

  final String message;
  final IconData icon;
  final VoidCallback? retry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: CinemaColors.muted, size: 48),
            const SizedBox(height: 16),
            Text(message, textAlign: TextAlign.center),
            if (retry != null) ...[
              const SizedBox(height: 16),
              FilledButton(onPressed: retry, child: const Text('Retry')),
            ],
          ],
        ),
      ),
    );
  }
}

class CinemaBottomNavigation extends StatelessWidget {
  const CinemaBottomNavigation({
    super.key,
    required this.selectedIndex,
    this.onItemSelected,
  });

  final int selectedIndex;
  final ValueChanged<int>? onItemSelected;

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: selectedIndex.clamp(0, 2),
      onDestinationSelected: onItemSelected,
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.movie_outlined),
          selectedIcon: Icon(Icons.movie),
          label: 'Movies',
        ),
        NavigationDestination(
          icon: Icon(Icons.confirmation_num_outlined),
          selectedIcon: Icon(Icons.confirmation_num),
          label: 'Tickets',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          selectedIcon: Icon(Icons.person),
          label: 'Account',
        ),
      ],
    );
  }
}

String formatVnd(int amount) {
  final digits = amount.toString();
  final groups = <String>[];

  for (var end = digits.length; end > 0; end -= 3) {
    groups.insert(0, digits.substring((end - 3).clamp(0, end).toInt(), end));
  }

  return '${groups.join('.')} ₫';
}

class CinemaPoster extends StatelessWidget {
  const CinemaPoster(
    this.asset, {
    super.key,
    this.width = 96,
    this.height = 128,
  });

  final String asset;
  final double width;
  final double height;

  Widget _placeholder() => Container(
    width: width,
    height: height,
    color: CinemaColors.surface,
    alignment: Alignment.center,
    child: const Icon(
      Icons.image_not_supported_outlined,
      color: CinemaColors.muted,
    ),
  );

  @override
  Widget build(BuildContext context) {
    final source = asset.trim();
    final uri = Uri.tryParse(source);
    final isNetwork = uri?.scheme == 'http' || uri?.scheme == 'https';

    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: source.isEmpty
          ? _placeholder()
          : isNetwork
          ? Image.network(
              source,
              width: width,
              height: height,
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) => _placeholder(),
            )
          : Image.asset(
              source,
              width: width,
              height: height,
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) => _placeholder(),
            ),
    );
  }
}

class SectionHeading extends StatelessWidget {
  const SectionHeading(this.title, {super.key});

  final String title;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12, top: 12),
    child: Text(
      title.toUpperCase(),
      style: Theme.of(context).textTheme.titleLarge
          ?.copyWith(fontWeight: FontWeight.w800),
    ),
  );
}

class PriceLine extends StatelessWidget {
  const PriceLine(this.label, this.value, {super.key, this.total = false});

  final String label;
  final int value;
  final bool total;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            color: total ? CinemaColors.text : CinemaColors.muted,
            fontWeight: total ? FontWeight.bold : null,
          ),
        ),
        Text(
          formatVnd(value),
          style: TextStyle(
            color: total ? CinemaColors.red : CinemaColors.text,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    ),
  );
}
