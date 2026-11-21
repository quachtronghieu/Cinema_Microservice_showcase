package vn.edu.fpt.cinema.cinema.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import vn.edu.fpt.cinema.cinema.client.BookingClient;
import vn.edu.fpt.cinema.cinema.client.CatalogMovieClient;
import vn.edu.fpt.cinema.cinema.entities.Room;
import vn.edu.fpt.cinema.cinema.entities.Showtime;
import vn.edu.fpt.cinema.cinema.entities.Template;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;
import vn.edu.fpt.cinema.cinema.enums.SeatType;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;
import vn.edu.fpt.cinema.cinema.repositories.RoomRepository;
import vn.edu.fpt.cinema.cinema.repositories.ShowtimeRepository;

/** Open/close sales: booking must confirm before cinema changes what it shows (AGENTS 4). */
class ShowtimeSalesTest {
    ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    ShowtimeSeatService seats = mock(ShowtimeSeatService.class);
    CatalogMovieClient catalog = mock(CatalogMovieClient.class);
    BookingClient booking = mock(BookingClient.class);
    ShowtimeService service = new ShowtimeService(showtimes, mock(RoomRepository.class), seats, catalog,
        mock(RoomService.class), booking);
    Showtime showtime = new Showtime();

    @BeforeEach void draftShowtimeInRoomWithTwoSeats() {
        Template template = new Template("TM1", "2D", "");
        showtime.setShowtimeId("ST1");
        showtime.setMovieId("MV1");
        showtime.setRoom(new Room(template, "ACTIVE"));
        showtime.setShowDate(LocalDate.now().plusDays(2));
        showtime.setStartTime(LocalTime.of(19, 0));
        showtime.setEndTime(LocalTime.of(21, 0));
        showtime.setSalesStatus("DRAFT");
        when(showtimes.findByShowtimeId("ST1")).thenReturn(showtime);
        when(showtimes.save(any())).thenAnswer(call -> call.getArgument(0));
        when(seats.physicalSeats("TM1")).thenReturn(List.of(
            new TemplateSeat("TS1", template, "A", 1, SeatType.STANDARD), new TemplateSeat("TS2", template, "A", 2, SeatType.VIP)));
        when(catalog.getMovieById("MV1")).thenReturn(new CatalogMovieClient.MovieMetadataDTO("MV1", "Dune", 120, "Sci-Fi", "", "T13"));
    }

    @Test void open_sendsRealSnapshotToBooking_thenShowsOpen() {
        assertThat(service.openShowtimeSales("ST1").salesStatus()).isEqualTo("OPEN");
        var sent = ArgumentCaptor.forClass(BookingClient.InitSalesRequest.class);
        verify(booking).initSales(sent.capture());
        assertThat(sent.getValue().movieTitle()).isEqualTo("Dune");
        assertThat(sent.getValue().seats()).extracting(BookingClient.InitSalesRequest.SeatItem::unitPrice)
            .containsExactly(new java.math.BigDecimal("80000.00"), new java.math.BigDecimal("100000.00"));
    }

    @Test void open_bookingDown_staysDraft() {
        doThrow(new CinemaException(HttpStatus.SERVICE_UNAVAILABLE, "BOOKING_UNAVAILABLE", "down")).when(booking).initSales(any());
        assertThatThrownBy(() -> service.openShowtimeSales("ST1")).isInstanceOf(CinemaException.class)
            .extracting("code").isEqualTo("BOOKING_UNAVAILABLE");
        assertThat(showtime.getSalesStatus()).isEqualTo("DRAFT");
        verify(showtimes, never()).save(any());
    }

    @Test void open_bookingRefusesClosedShowtime_409_staysAsItWas() {
        showtime.setSalesStatus("CLOSED");
        doThrow(new CinemaException(HttpStatus.CONFLICT, "SALES_CLOSED", "closed")).when(booking).initSales(any());
        assertThatThrownBy(() -> service.openShowtimeSales("ST1")).extracting("code").isEqualTo("SALES_CLOSED");
        assertThat(showtime.getSalesStatus()).isEqualTo("CLOSED");
    }

    @Test void open_withoutRealMovieTitle_neverReachesBooking() {
        when(catalog.getMovieById("MV1")).thenReturn(null);
        assertThatThrownBy(() -> service.openShowtimeSales("ST1")).extracting("code").isEqualTo("MOVIE_TITLE_UNAVAILABLE");
        verifyNoInteractions(booking);
        assertThat(showtime.getSalesStatus()).isEqualTo("DRAFT");
    }

    @Test void close_bookingClosesFirst_thenShowsClosed() {
        showtime.setSalesStatus("OPEN");
        assertThat(service.closeShowtimeSales("ST1").salesStatus()).isEqualTo("CLOSED");
        var order = inOrder(booking, showtimes);
        order.verify(booking).closeSales("ST1");
        order.verify(showtimes).save(showtime);
    }

    @Test void close_bookingDown_staysOpen() {
        showtime.setSalesStatus("OPEN");
        doThrow(new CinemaException(HttpStatus.SERVICE_UNAVAILABLE, "BOOKING_UNAVAILABLE", "down")).when(booking).closeSales("ST1");
        assertThatThrownBy(() -> service.closeShowtimeSales("ST1")).isInstanceOf(CinemaException.class);
        assertThat(showtime.getSalesStatus()).isEqualTo("OPEN");
        verify(showtimes, never()).save(any());
    }
}
