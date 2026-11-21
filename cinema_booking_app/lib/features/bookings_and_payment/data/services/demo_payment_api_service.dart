import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';

class DemoPaymentApiService {
  DemoPaymentApiService({Dio? dio}) : _dio = dio ?? ApiClient.instance.dio;

  final Dio _dio;

  Future<Response<dynamic>> process({
    required String bookingId,
    required Map<String, dynamic> request,
  }) => _dio.post(ApiConfig.customerDemoPayment(bookingId), data: request);
}
