import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';

class WatchlistApiService {
  WatchlistApiService({Dio? dio}) : _dio = dio ?? ApiClient.instance.dio;

  final Dio _dio;

  // Không theo redirect: khi hết phiên Spring chuyển sang trang /login và
  // Dio sẽ nhận 200 HTML, khiến thao tác ghi trông như đã thành công.
  static final _options = Options(followRedirects: false);

  Future<Response<dynamic>> getWatchlist() =>
      _dio.get(ApiConfig.customerWatchlist, options: _options);

  Future<Response<dynamic>> add(String movieId) =>
      _dio.put(ApiConfig.customerWatchlistItem(movieId), options: _options);

  Future<Response<dynamic>> remove(String movieId) =>
      _dio.delete(ApiConfig.customerWatchlistItem(movieId), options: _options);
}
