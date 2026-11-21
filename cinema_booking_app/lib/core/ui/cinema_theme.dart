import 'package:flutter/material.dart';

class CinemaColors {
  static const background = Color(0xFF121212);
  static const surface = Color(0xFF1E1E1E);
  static const red = Color(0xFFE71A0F);
  static const text = Color(0xFFEDEDED);
  static const muted = Color(0xFF9E9E9E);
  static const warning = Color(0xFFFFB800);
}

ThemeData get cinemaDarkTheme {
  final colorScheme =
      ColorScheme.fromSeed(
        seedColor: CinemaColors.red,
        brightness: Brightness.dark,
      ).copyWith(
        primary: CinemaColors.red,
        onPrimary: Colors.white,
        surface: CinemaColors.surface,
        onSurface: CinemaColors.text,
        error: CinemaColors.red,
      );

  return ThemeData(
    colorScheme: colorScheme,
    brightness: Brightness.dark,
    useMaterial3: true,
    scaffoldBackgroundColor: CinemaColors.background,
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
  );
}
