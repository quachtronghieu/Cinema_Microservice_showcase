import 'package:url_launcher/url_launcher.dart';

/// Mở link trailer ngoài app (YouTube hoặc trình duyệt).
abstract interface class TrailerLauncher {
  /// Trả `false` khi thiết bị không có ứng dụng mở được [url].
  Future<bool> open(String url);
}

class UrlLauncherTrailerLauncher implements TrailerLauncher {
  const UrlLauncherTrailerLauncher();

  @override
  Future<bool> open(String url) async {
    final uri = Uri.tryParse(url);
    if (uri == null || !uri.hasScheme) return false;
    try {
      return await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      return false;
    }
  }
}
