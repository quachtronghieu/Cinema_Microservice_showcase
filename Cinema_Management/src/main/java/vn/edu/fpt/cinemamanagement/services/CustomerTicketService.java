package vn.edu.fpt.cinemamanagement.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cinemamanagement.dto.response.CustomerTicketResponseDTO;
import vn.edu.fpt.cinemamanagement.entities.BookingDetail;
import vn.edu.fpt.cinemamanagement.entities.Concession;
import vn.edu.fpt.cinemamanagement.entities.Showtime;
import vn.edu.fpt.cinemamanagement.entities.ShowtimeSeat;
import vn.edu.fpt.cinemamanagement.entities.Ticket;
import vn.edu.fpt.cinemamanagement.repositories.BookingDetailRepository;
import vn.edu.fpt.cinemamanagement.repositories.TicketRepository;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/** Reads only tickets that were issued for the authenticated customer. */
@Service
public class CustomerTicketService {
    private final TicketRepository ticketRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public CustomerTicketService(
            TicketRepository ticketRepository,
            BookingDetailRepository bookingDetailRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    @Transactional
    public List<CustomerTicketResponseDTO> findAllForCustomer(String customerId) {
        return ticketRepository.findByBooking_UserIdOrderByIdDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerTicketResponseDTO findForCustomer(String customerId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy vé."));
        if (ticket.getBooking() == null || !customerId.equals(ticket.getBooking().getUserId())) {
            throw new SecurityException("Bạn không có quyền xem vé này.");
        }
        return toResponse(ticket);
    }

    private CustomerTicketResponseDTO toResponse(Ticket ticket) {
        List<BookingDetail> details = bookingDetailRepository.findByBooking(ticket.getBooking());
        List<BookingDetail> seatDetails = details.stream()
                .filter(detail -> detail.getShowtimeSeat() != null)
                .toList();
        if (seatDetails.isEmpty()) {
            throw new IllegalStateException("Vé không có thông tin ghế hợp lệ.");
        }

        Showtime showtime = seatDetails.getFirst().getShowtimeSeat().getShowtime();
        List<String> seatLabels = seatDetails.stream()
                .map(BookingDetail::getShowtimeSeat)
                .sorted(Comparator
                        .comparing((ShowtimeSeat seat) -> seat.getTemplateSeat().getRowLabel())
                        .thenComparing(seat -> seat.getTemplateSeat().getSeatNumber()))
                .map(seat -> seat.getTemplateSeat().getRowLabel() + seat.getTemplateSeat().getSeatNumber())
                .toList();
        List<CustomerTicketResponseDTO.ConcessionItem> concessionItems = details.stream()
                .filter(detail -> detail.getConcession() != null)
                .map(detail -> toConcessionItem(detail, detail.getConcession()))
                .toList();

        return new CustomerTicketResponseDTO(
                ticket.getId(),
                ticket.getBooking().getId(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getStartTime(),
                showtime.getRoom().getId(),
                seatLabels,
                concessionItems,
                ticket.getPrice(),
                ticket.isRedemptionStatus()
        );
    }

    private CustomerTicketResponseDTO.ConcessionItem toConcessionItem(
            BookingDetail detail,
            Concession concession
    ) {
        int quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
        java.math.BigDecimal unitPrice = detail.getUnitPrice() == null
                ? concession.getPrice()
                : detail.getUnitPrice();
        return new CustomerTicketResponseDTO.ConcessionItem(
                concession.getConcessionId(),
                concession.getName(),
                quantity,
                unitPrice,
                unitPrice.multiply(java.math.BigDecimal.valueOf(quantity))
        );
    }
}
