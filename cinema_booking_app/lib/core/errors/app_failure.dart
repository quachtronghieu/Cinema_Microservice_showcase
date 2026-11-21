enum AppFailureType {
  sessionExpired,
  forbidden,
  network,
  invalidData,
  server,
  unknown,
}

class AppFailure implements Exception {
  const AppFailure({required this.type, required this.message, this.cause});

  final AppFailureType type;
  final String message;
  final Object? cause;

  @override
  String toString() => message;
}
