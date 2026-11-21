import '../../../../core/network/api_result.dart';
import '../entities/customer_ticket.dart';

abstract interface class CustomerTicketRepository {
  Future<ApiResult<List<CustomerTicket>>> findAll();
  Future<ApiResult<CustomerTicket>> findById(String ticketId);
}
