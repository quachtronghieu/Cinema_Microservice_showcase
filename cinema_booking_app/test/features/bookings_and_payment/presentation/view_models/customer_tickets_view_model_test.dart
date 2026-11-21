import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/entities/customer_ticket.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/domain/repositories/customer_ticket_repository.dart';
import 'package:cinema_booking_app/features/bookings_and_payment/presentation/view_models/customer_tickets_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('marks a ticket expired from its showtime, not from payment status', () {
    expect(
      _FakeTicketRepository._ticket.isExpiredAt(DateTime(2026, 10, 17, 19)),
      isTrue,
    );
    expect(
      _FakeTicketRepository._ticket.isExpiredAt(DateTime(2026, 10, 17, 18, 59)),
      isFalse,
    );
  });

  test('loads tickets issued for the signed-in customer', () async {
    final viewModel = CustomerTicketsViewModel(_FakeTicketRepository());

    await viewModel.loadTickets();

    expect(viewModel.error, isNull);
    expect(viewModel.tickets, hasLength(1));
    expect(viewModel.tickets.single.ticketId, 'TK000001');
  });

  test('loads a server-issued ticket by id', () async {
    final viewModel = CustomerTicketsViewModel(_FakeTicketRepository());

    await viewModel.loadTicket('TK000001');

    expect(viewModel.error, isNull);
    expect(viewModel.ticket?.movieTitle, 'Phim thử nghiệm');
    expect(viewModel.ticket?.seatLabels, ['A1', 'A2']);
  });
}

class _FakeTicketRepository implements CustomerTicketRepository {
  static final _ticket = CustomerTicket(
    ticketId: 'TK000001',
    bookingId: 'BK000001',
    movieTitle: 'Phim thử nghiệm',
    showDate: DateTime(2026, 10, 17),
    startTime: '19:00:00',
    roomId: '1',
    seatLabels: ['A1', 'A2'],
    concessionItems: const [
      CustomerTicketConcession(
        concessionId: 'CC000001',
        name: 'Bap rang bo',
        quantity: 1,
        unitPrice: 45000,
        lineTotal: 45000,
      ),
    ],
    totalAmount: 180000,
    checkedIn: false,
  );

  @override
  Future<ApiResult<List<CustomerTicket>>> findAll() =>
      Future.value(ApiResult.success([_ticket]));

  @override
  Future<ApiResult<CustomerTicket>> findById(String ticketId) =>
      Future.value(ApiResult.success(_ticket));
}
