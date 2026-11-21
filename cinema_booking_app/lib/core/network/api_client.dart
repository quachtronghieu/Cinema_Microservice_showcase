import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart';

import '../config/api_config.dart';

class ApiClient {
  ApiClient._(this.dio, this.cookieJar);

  final Dio dio;
  final CookieJar cookieJar;
  static ApiClient? _instance;
  static final sessionExpired = ValueNotifier<bool>(false);

  static Future<ApiClient> getInstance({Dio? dio, CookieJar? cookieJar}) async {
    if (_instance != null) {
      return _instance!;
    }

    if (cookieJar == null) {
      if (kIsWeb) {
        // Trình duyệt tự quản lý cookie và không có thư mục file của ứng dụng.
        cookieJar = CookieJar();
      } else {
        final directory = await getApplicationDocumentsDirectory();
        cookieJar = PersistCookieJar(
          storage: FileStorage('${directory.path}/.cookies/'),
        );
      }
    }
    final configuredDio =
        dio ??
        Dio(
          BaseOptions(
            baseUrl: ApiConfig.baseUrl,
            connectTimeout: const Duration(seconds: 30),
            receiveTimeout: const Duration(seconds: 30),
            sendTimeout: const Duration(seconds: 30),
          ),
        );

    if (!kIsWeb) configuredDio.interceptors.add(CookieManager(cookieJar));

    configuredDio.interceptors.add(
      InterceptorsWrapper(
        onError: (error, handler) {
          final sentToken = error.requestOptions.headers['Authorization'];
          final currentToken = configuredDio.options.headers['Authorization'];

          if (error.response?.statusCode == 401 &&
              sentToken is String &&
              sentToken.startsWith('Bearer ') &&
              sentToken == currentToken) {
            configuredDio.options.headers.remove('Authorization');
            sessionExpired.value = true;
          }

          handler.next(error);
        },
      ),
    );

    return _instance = ApiClient._(configuredDio, cookieJar);
  }

  static ApiClient get instance {
    final client = _instance;
    if (client == null) {
      throw StateError(
        'ApiClient has not been initialized. '
        'Call ApiClient.getInstance() first.',
      );
    }
    return client;
  }

  static void reset() {
    _instance = null;
  }
}
