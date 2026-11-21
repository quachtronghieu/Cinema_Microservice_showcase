class CustomerTicket {
  const CustomerTicket({
    required this.ticketId,
    required this.bookingId,
    required this.movieTitle,
    required this.showDate,
    required this.startTime,
    required this.roomId,
    required this.seatLabels,
    required this.concessionItems,
    required this.totalAmount,
    required this.checkedIn,
  });

  final String ticketId;
  final String bookingId;
  final String movieTitle;
  final DateTime showDate;
  final String startTime;
  final String roomId;
  final List<String> seatLabels;
  final List<CustomerTicketConcession> concessionItems;
  final int totalAmount;
  final bool checkedIn;

  /// Local showtime supplied by the server. A ticket is no longer usable from
  /// the moment its showtime begins, but it remains available as a record.
  DateTime get showtimeStartsAt {
    final parts = startTime.split(':').map(int.tryParse).toList();
    return DateTime(
      showDate.year,
      showDate.month,
      showDate.day,
      parts.isNotEmpty ? parts[0] ?? 0 : 0,
      parts.length > 1 ? parts[1] ?? 0 : 0,
      parts.length > 2 ? parts[2] ?? 0 : 0,
    );
  }

  bool isExpiredAt(DateTime now) => !now.isBefore(showtimeStartsAt);

  bool get isExpired => isExpiredAt(DateTime.now());
}

class CustomerTicketConcession {
  const CustomerTicketConcession({
    required this.concessionId,
    required this.name,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  final String concessionId;
  final String name;
  final int quantity;
  final int unitPrice;
  final int lineTotal;
}
