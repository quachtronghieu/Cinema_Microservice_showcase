import 'package:flutter/foundation.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_result.dart';
import '../../data/repositories/customer_ticket_repository_impl.dart';
import '../../data/services/customer_ticket_api_service.dart';
import '../../domain/entities/customer_ticket.dart';
import '../../domain/repositories/customer_ticket_repository.dart';

class CustomerTicketsViewModel extends ChangeNotifier {
  CustomerTicketsViewModel([CustomerTicketRepository? repository])
    : _repository =
          repository ??
          CustomerTicketRepositoryImpl(
            CustomerTicketApiService(dio: ApiClient.instance.dio),
          );

  final CustomerTicketRepository _repository;
  List<CustomerTicket> _tickets = const [];
  CustomerTicket? _ticket;
  String? _error;
  bool _loading = false;

  List<CustomerTicket> get tickets => _tickets;
  CustomerTicket? get ticket => _ticket;
  String? get error => _error;
  bool get loading => _loading;

  Future<void> loadTickets() async {
    _loading = true;
    _error = null;
    notifyListeners();
    final response = await _repository.findAll();
    _loading = false;
    if (response case Success<List<CustomerTicket>>(:final data)) {
      _tickets = data;
    } else {
      _error =
          (response as FailureResult<List<CustomerTicket>>).failure.message;
    }
    notifyListeners();
  }

  Future<void> loadTicket(String ticketId) async {
    _loading = true;
    _error = null;
    _ticket = null;
    notifyListeners();
    final response = await _repository.findById(ticketId);
    _loading = false;
    if (response case Success<CustomerTicket>(:final data)) {
      _ticket = data;
    } else {
      _error = (response as FailureResult<CustomerTicket>).failure.message;
    }
    notifyListeners();
  }
}
