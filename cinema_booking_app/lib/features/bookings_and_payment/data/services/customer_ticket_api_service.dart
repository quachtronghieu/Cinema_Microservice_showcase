import 'package:dio/dio.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';

class CustomerTicketApiService {
  CustomerTicketApiService({Dio? dio}) : _dio = dio ?? ApiClient.instance.dio;

  final Dio _dio;

  Future<Response<dynamic>> findAll() => _dio.get(ApiConfig.customerTickets);

  Future<Response<dynamic>> findById(String ticketId) =>
      _dio.get(ApiConfig.customerTicket(ticketId));
}
