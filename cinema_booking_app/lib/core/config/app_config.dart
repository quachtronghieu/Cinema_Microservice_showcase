class AppConfig {
  const AppConfig._();

  static const apiBaseUrl = String.fromEnvironment('API_BASE_URL');
  static const useConcessionApi = bool.fromEnvironment(
    'USE_CONCESSION_API',
    defaultValue: false,
  );
}
