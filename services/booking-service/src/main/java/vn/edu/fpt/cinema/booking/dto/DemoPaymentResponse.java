package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;

/** Field names match the legacy response the Flutter payment screen reads. */
public record DemoPaymentResponse(String bookingId, String paymentId, String paymentStatus, BigDecimal amount,
                                  String ticketId, String message) {}
