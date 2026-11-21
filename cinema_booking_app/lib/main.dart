import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'app/app_providers.dart';
import 'core/config/api_config.dart';
import 'core/config/app_config.dart';
import 'core/network/api_client.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('vi_VN');

  ApiConfig.configure(baseUrl: AppConfig.apiBaseUrl);
  await ApiClient.getInstance();

  runApp(const AppProviders(child: CinemaApp()));
}
