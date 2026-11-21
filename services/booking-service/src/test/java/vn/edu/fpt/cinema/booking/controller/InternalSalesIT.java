package vn.edu.fpt.cinema.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class InternalSalesIT extends SqlServerIT {
    static final String CINEMA = TestJwt.service("booking-service", "sales:write");

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    String showDate = LocalDate.now().plusDays(3).toString();

    @BeforeEach void clean() { DbReset.clean(jdbc); }

    private String initBody() {
        return """
            {"showtimeId":"ST1","movieId":"MV1","movieTitle":"Dune","roomId":1,"roomName":"2D",
             "showDate":"%s","startTime":"19:00:00","endTime":"21:00:00",
             "seats":[{"templateSeatId":"TS1","rowLabel":"A","seatNumber":1,"seatType":"standard","unitPrice":80000},
                      {"templateSeatId":"TS2","rowLabel":"A","seatNumber":2,"seatType":"VIP","unitPrice":100000}]}
            """.formatted(showDate);
    }

    private ResultActions init(String authorization, String body) throws Exception {
        return mvc.perform(post("/api/v1/internal/showtimes/init-sales").header("Authorization", authorization)
            .contentType("application/json").content(body));
    }

    private ResultActions close(String authorization) throws Exception {
        return mvc.perform(post("/api/v1/internal/showtimes/ST1/close-sales").header("Authorization", authorization));
    }

    private String salesStatus() {
        return jdbc.queryForObject("SELECT sales_status FROM dbo.Sales_Snapshot WHERE showtime_id = 'ST1'", String.class);
    }

    private int seats() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime_Seat WHERE showtime_id = 'ST1'", Integer.class);
    }

    @Test void init_createsOpenSnapshotAndSeats_retryChangesNothing() throws Exception {
        init(CINEMA, initBody()).andExpect(status().isOk()).andExpect(jsonPath("$.salesStatus").value("OPEN"));
        assertThat(salesStatus()).isEqualTo("OPEN");
        assertThat(jdbc.queryForList("SELECT seat_type FROM dbo.Showtime_Seat WHERE showtime_id = 'ST1' AND status = 'AVAILABLE' ORDER BY seat_number", String.class))
            .containsExactly("STANDARD", "VIP");

        init(CINEMA, initBody()).andExpect(status().isOk());
        assertThat(seats()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT snapshot_version FROM dbo.Sales_Snapshot WHERE showtime_id = 'ST1'", Long.class)).isEqualTo(1L);
    }

    @Test void customerCanHoldAfterInit_butNotAfterClose() throws Exception {
        init(CINEMA, initBody()).andExpect(status().isOk());
        String seat = jdbc.queryForObject("SELECT showtime_seat_id FROM dbo.Showtime_Seat WHERE template_seat_id = 'TS1'", String.class);
        String hold = "{\"showtimeId\":\"ST1\",\"showtimeSeatIds\":[\"" + seat + "\"]}";
        mvc.perform(post("/api/v1/customer/seat-holds").header("Authorization", TestJwt.customer("CS1"))
            .contentType("application/json").content(hold)).andExpect(status().isCreated());

        close(CINEMA).andExpect(status().isOk()).andExpect(jsonPath("$.salesStatus").value("CLOSED"));
        mvc.perform(post("/api/v1/customer/seat-holds").header("Authorization", TestJwt.customer("CS2"))
            .contentType("application/json").content(hold)).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SALES_CLOSED"));
    }

    @Test void closedShowtime_isNeverReopened_409() throws Exception {
        init(CINEMA, initBody()).andExpect(status().isOk());
        close(CINEMA).andExpect(status().isOk());
        init(CINEMA, initBody()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALES_CLOSED"));
        assertThat(salesStatus()).isEqualTo("CLOSED");
        close(CINEMA).andExpect(status().isOk()); // repeating a close is harmless
    }

    @Test void closeUnknownShowtime_isHarmless() throws Exception {
        close(CINEMA).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Sales_Snapshot", Integer.class)).isZero();
    }

    @Test void onlyServiceTokenWithSalesWrite_noToken401_userOrAdmin403_wrongScopeOrAudience() throws Exception {
        mvc.perform(post("/api/v1/internal/showtimes/init-sales").contentType("application/json").content(initBody()))
            .andExpect(status().isUnauthorized());
        init(TestJwt.customer("CS1"), initBody()).andExpect(status().isForbidden());
        init(TestJwt.admin("ST1"), initBody()).andExpect(status().isForbidden());
        init(TestJwt.service("booking-service", "showtimes:read"), initBody()).andExpect(status().isForbidden());
        // A service token minted for another service is not valid here at all.
        init(TestJwt.service("catalog-service", "sales:write"), initBody()).andExpect(status().isUnauthorized());
        close(TestJwt.customer("CS1")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Sales_Snapshot", Integer.class)).isZero();
    }

    @Test void serviceToken_cannotActAsCustomer() throws Exception {
        mvc.perform(post("/api/v1/customer/seat-holds").header("Authorization", CINEMA)
            .contentType("application/json").content("{\"showtimeId\":\"ST1\",\"showtimeSeatIds\":[\"x\"]}"))
            .andExpect(status().isForbidden());
    }

    @Test void invalidSnapshot_400_storesNothing() throws Exception {
        init(CINEMA, initBody().replace("\"unitPrice\":80000", "\"unitPrice\":0")).andExpect(status().isBadRequest());
        init(CINEMA, initBody().replace("\"movieTitle\":\"Dune\",", "")).andExpect(status().isBadRequest());
        init(CINEMA, initBody().replaceAll("\"seats\":\\[[\\s\\S]*\\]", "\"seats\":[]")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Sales_Snapshot", Integer.class)).isZero();
    }
}
