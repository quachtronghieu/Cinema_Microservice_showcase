package vn.edu.fpt.cinemamanagement.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cinemamanagement.dto.response.DemoPaymentResponseDTO;
import vn.edu.fpt.cinemamanagement.entities.Booking;
import vn.edu.fpt.cinemamanagement.entities.BookingDetail;
import vn.edu.fpt.cinemamanagement.entities.Payment;
import vn.edu.fpt.cinemamanagement.entities.ShowtimeSeat;
import vn.edu.fpt.cinemamanagement.entities.Ticket;
import vn.edu.fpt.cinemamanagement.enums.BookingStatus;
import vn.edu.fpt.cinemamanagement.enums.SeatStatus;
import vn.edu.fpt.cinemamanagement.repositories.BookingDetailRepository;
import vn.edu.fpt.cinemamanagement.repositories.BookingRepository;
import vn.edu.fpt.cinemamanagement.repositories.PaymentRepository;
import vn.edu.fpt.cinemamanagement.repositories.ShowtimeSeatRepository;
import vn.edu.fpt.cinemamanagement.repositories.TicketRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/** Applies the two outcomes used by the mobile demo payment screen. */
@Service
public class CustomerDemoPaymentService {
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TicketRepository ticketRepository;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final CustomerBookingCancellationService cancellationService;

    public CustomerDemoPaymentService(
            BookingRepository bookingRepository,
            BookingDetailRepository bookingDetailRepository,
            PaymentRepository paymentRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            TicketRepository ticketRepository,
            PaymentService paymentService,
            TicketService ticketService,
            CustomerBookingCancellationService cancellationService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.paymentRepository = paymentRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.ticketRepository = ticketRepository;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.cancellationService = cancellationService;
    }

    @Transactional
    public synchronized DemoPaymentResponseDTO process(
            String customerId,
            String bookingId,
            String requestedOutcome
    ) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng."));
        if (!customerId.equals(booking.getUserId())) {
            throw new SecurityException("Bạn không có quyền thanh toán đơn hàng này.");
        }

        DemoOutcome outcome = DemoOutcome.from(requestedOutcome);
        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã hủy, không thể thanh toán.");
        }
        if (BookingStatus.PAID.equals(booking.getStatus())) {
            return paidResult(booking);
        }
        if (!BookingStatus.BOOKED.equals(booking.getStatus())) {
            throw new IllegalStateException("Đơn hàng chưa sẵn sàng để thanh toán.");
        }

        Payment payment = paymentRepository.findByBooking_Id(bookingId)
                .orElseGet(() -> newPayment(booking));
        payment.setPaymentMethod("DEMO");
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentTime(LocalDateTime.now());

        if (isExpired(booking)) {
            payment.setPaymentStatus("EXPIRED");
            paymentRepository.save(payment);
            cancellationService.cancelBookedBooking(booking);
            return new DemoPaymentResponseDTO(
                    booking.getId(), payment.getId(), payment.getPaymentStatus(),
                    payment.getAmount(), null,
                    "Đơn hàng đã hết hạn thanh toán sau 5 phút. Ghế đã được trả lại."
            );
        }

        if (outcome == DemoOutcome.FAILED) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
            cancellationService.cancelBookedBooking(booking);
            return new DemoPaymentResponseDTO(
                    booking.getId(), payment.getId(), payment.getPaymentStatus(),
                    payment.getAmount(), null,
                    "Thanh toán demo thất bại. Đơn hàng đã hủy và ghế đã được trả lại."
            );
        }

        payment.setPaymentStatus("PAID");
        paymentRepository.save(payment);
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        List<BookingDetail> details = bookingDetailRepository.findByBooking(booking);
        List<ShowtimeSeat> seats = details.stream()
                .map(BookingDetail::getShowtimeSeat)
                .filter(seat -> seat != null)
                .toList();
        seats.forEach(seat -> seat.setStatus(SeatStatus.UNAVAILABLE));
        showtimeSeatRepository.saveAll(seats);

        Ticket ticket = ticketRepository.findByBookingId(booking.getId());
        if (ticket == null) {
            ticket = new Ticket();
            ticket.setId(ticketService.generateTicketId());
            ticket.setBooking(booking);
            ticket.setPrice(payment.getAmount());
            ticket.setRedemptionStatus(false);
            ticket.setCheckedInTime(null);
            ticket.setRedemptionStaff(null);
            ticketRepository.save(ticket);
        }
        return new DemoPaymentResponseDTO(
                booking.getId(), payment.getId(), payment.getPaymentStatus(),
                payment.getAmount(), ticket.getId(),
                "Thanh toán demo thành công. Vé đã được phát hành."
        );
    }

    private Payment newPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setId(paymentService.generatePaymentId());
        payment.setBooking(booking);
        return payment;
    }

    private boolean isExpired(Booking booking) {
        return booking.getCreatedAt() != null
                && !booking.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5));
    }

    private DemoPaymentResponseDTO paidResult(Booking booking) {
        Payment payment = paymentRepository.findByBooking_Id(booking.getId())
                .orElseThrow(() -> new IllegalStateException("Đơn đã thanh toán nhưng không có giao dịch."));
        Ticket ticket = ticketRepository.findByBookingId(booking.getId());
        return new DemoPaymentResponseDTO(
                booking.getId(), payment.getId(), payment.getPaymentStatus(),
                payment.getAmount(), ticket == null ? null : ticket.getId(),
                "Đơn hàng đã được thanh toán trước đó."
        );
    }

    private enum DemoOutcome {
        SUCCESS,
        FAILED;

        static DemoOutcome from(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Kết quả thanh toán demo là bắt buộc.");
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Kết quả thanh toán demo không hợp lệ.");
            }
        }
    }
}
