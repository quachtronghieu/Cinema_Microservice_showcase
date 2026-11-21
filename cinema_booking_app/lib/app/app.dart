import 'package:flutter/material.dart';

import 'app_router.dart';
import 'app_theme.dart';

class CinemaApp extends StatelessWidget {
  const CinemaApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    routerConfig: appRouter,
    theme: cinemaDarkTheme,
    debugShowCheckedModeBanner: false,
  );
}
