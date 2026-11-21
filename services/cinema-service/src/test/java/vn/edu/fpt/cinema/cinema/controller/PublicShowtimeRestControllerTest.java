package vn.edu.fpt.cinema.cinema.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import vn.edu.fpt.cinema.cinema.config.SecurityConfiguration;
import vn.edu.fpt.cinema.cinema.support.TestJwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeResponseDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeSeatMapResponse;
import vn.edu.fpt.cinema.cinema.exception.ShowtimeNotFoundException;
import vn.edu.fpt.cinema.cinema.service.ShowtimeService;

@WebMvcTest(controllers = PublicShowtimeRestController.class, properties = {
    "cinema.security.issuer=" + TestJwt.ISSUER, "cinema.security.jwk-set-uri=https://issuer.test/jwks",
    "cinema.security.audience=" + TestJwt.AUDIENCE})
@Import({SecurityConfiguration.class, TestJwt.Keys.class})
class PublicShowtimeRestControllerTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 4);
    static final String MOVIE = "a563f4f6-e6aa-49ae-b0a1-7e22ee6e4325";

    @Autowired MockMvc mvc;
    @MockitoBean ShowtimeService showtimes;

    static ShowtimeResponseDTO showtime() {
        return new ShowtimeResponseDTO("ST000001", MOVIE, "THE ODYSSEY", "1", DAY,
            LocalTime.of(19, 30), LocalTime.of(22, 15), null, "OPEN");
    }

    @Test void movieAndDateFilterTogether() throws Exception {
        when(showtimes.getCustomerShowtimes(MOVIE, DAY)).thenReturn(List.of(showtime()));

        mvc.perform(get("/api/v1/showtimes").param("movieId", MOVIE).param("date", "2026-11-04"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].showtimeId").value("ST000001"))
            .andExpect(jsonPath("$[0].showDate").value("2026-11-04"))
            .andExpect(jsonPath("$[0].startTime").value("19:30:00"));

        verify(showtimes).getCustomerShowtimes(MOVIE, DAY);
        verifyNoMoreInteractions(showtimes);
    }

    @Test void seatMapIsPhysicalLayoutWithoutAvailability() throws Exception {
        when(showtimes.getSeatMap("ST000001")).thenReturn(new ShowtimeSeatMapResponse("ST000001", MOVIE, "THE ODYSSEY", "1", "TM000001",
            DAY, LocalTime.of(19, 30), List.of(new ShowtimeSeatMapResponse.Seat("TS000001", "A", 1, "STANDARD"))));

        mvc.perform(get("/api/v1/showtimes/ST000001/seats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value("TM000001"))
            .andExpect(jsonPath("$.roomId").value("1"))
            .andExpect(jsonPath("$.seats[0].templateSeatId").value("TS000001"))
            .andExpect(jsonPath("$.seats[0].status").doesNotExist())
            .andExpect(jsonPath("$.seats[0].showtimeSeatId").doesNotExist());
    }

    @Test void seatMapOfUnknownShowtimeIs404() throws Exception {
        when(showtimes.getSeatMap("NOPE")).thenThrow(new ShowtimeNotFoundException("missing"));

        mvc.perform(get("/api/v1/showtimes/NOPE/seats"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SHOWTIME_NOT_FOUND"));
    }

    @Test void movieAloneStillListsEveryDate() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", MOVIE)).andExpect(status().isOk());
        verify(showtimes).getCustomerShowtimes(MOVIE, null);
        verifyNoMoreInteractions(showtimes);
    }

    @Test void dateAloneStillListsEveryMovie() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("date", "2026-11-04")).andExpect(status().isOk());
        verify(showtimes).getCustomerShowtimes(null, DAY);
        verifyNoMoreInteractions(showtimes);
    }
}
