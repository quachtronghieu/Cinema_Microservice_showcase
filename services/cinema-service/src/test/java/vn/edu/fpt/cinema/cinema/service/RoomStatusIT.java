package vn.edu.fpt.cinema.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.edu.fpt.cinema.cinema.client.CatalogMovieClient;
import vn.edu.fpt.cinema.cinema.config.SecurityConfiguration;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeRequestDTO;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;
import vn.edu.fpt.cinema.cinema.support.TestJwt;

/** Real SQL Server with Flyway V1/V2: the room lock and the occupancy rule need real transactions. */
@SpringBootTest(properties = {"eureka.client.enabled=false",
    "cinema.security.issuer=" + TestJwt.ISSUER, "cinema.security.jwk-set-uri=https://issuer.test/jwks",
    "cinema.security.audience=" + TestJwt.AUDIENCE})
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
@Testcontainers(disabledWithoutDocker = true)
class RoomStatusIT {
    @Container static final MSSQLServerContainer<?> SQL =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04").acceptLicense();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL::getJdbcUrl);
        registry.add("spring.datasource.username", SQL::getUsername);
        registry.add("spring.datasource.password", SQL::getPassword);
        registry.add("spring.flyway.url", SQL::getJdbcUrl);
        registry.add("spring.flyway.user", SQL::getUsername);
        registry.add("spring.flyway.password", SQL::getPassword);
    }

    @MockitoBean CatalogMovieClient catalog;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ShowtimeService showtimes;

    static final String MOVIE = "MV000016";
    int room;

    @BeforeEach void rooms() {
        when(catalog.getMovieById(any())).thenReturn(new CatalogMovieClient.MovieMetadataDTO(MOVIE, "DEMO", 120, null, null, null));
        for (String table : new String[] {"Room_Status_Audit", "Showtime_Seat", "Showtime", "Room", "Template_Seat", "Template"}) {
            jdbc.update("DELETE FROM dbo." + table);
        }
        jdbc.update("INSERT INTO dbo.Template (template_id, name, description) VALUES ('TM000001', '2D', '')");
        jdbc.update("INSERT INTO dbo.Template_Seat VALUES ('TS1', 'TM000001', 'A', 1, 'STANDARD'), ('TS2', 'TM000001', 'A', 2, 'VIP'), ('TS3', 'TM000001', 'B', 1, 'VIP')");
        jdbc.update("INSERT INTO dbo.Room (template_id, status) VALUES ('TM000001', 'ACTIVE')");
        room = jdbc.queryForObject("SELECT MAX(room_id) FROM dbo.Room", Integer.class);
    }

    private ResultActions patchStatus(String status) throws Exception {
        return mvc.perform(patch("/api/v1/admin/rooms/" + room + "/status").header("Authorization", TestJwt.admin("ST000001"))
            .header("X-Trace-Id", "trace-" + status).contentType("application/json").content("{\"status\":\"" + status + "\"}"));
    }

    private void showtime(String id, LocalDateTime start, LocalTime end, String sales) {
        jdbc.update("INSERT INTO dbo.Showtime (showtime_id, movie_id, room_id, show_date, start_time, end_time, sales_status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, MOVIE, room, start.toLocalDate(), start.toLocalTime().withNano(0), end.withNano(0), sales);
    }

    private LocalDateTime now() { return LocalDateTime.now(SecurityConfiguration.CINEMA_ZONE).withSecond(0).withNano(0); }

    private int audits() { return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Room_Status_Audit", Integer.class); }

    private String roomStatus() { return jdbc.queryForObject("SELECT status FROM dbo.Room WHERE room_id = ?", String.class, room); }

    @Test void listDetailAndTemplate_readTheFixedLayout() throws Exception {
        String admin = TestJwt.admin("ST1");
        mvc.perform(get("/api/v1/admin/rooms").header("Authorization", admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].roomId").value(String.valueOf(room)))
            .andExpect(jsonPath("$.items[0].capacity").value(3)).andExpect(jsonPath("$.items[0].vipSeats").value(2))
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/rooms?size=101").header("Authorization", admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/rooms/" + room).header("Authorization", admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value("TM000001")).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/v1/admin/rooms/" + room + "/template").header("Authorization", admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.capacity").value(3))
            .andExpect(jsonPath("$.seats[0].templateSeatId").value("TS1")).andExpect(jsonPath("$.seats[2].rowLabel").value("B"));
        mvc.perform(get("/api/v1/admin/rooms/999").header("Authorization", admin)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
        mvc.perform(get("/api/v1/admin/rooms/R1").header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test void noFutureShowtime_changes_writesAudit_repeatIsNoOp() throws Exception {
        showtime("PAST", now().minusDays(3), LocalTime.of(12, 0), "OPEN");
        String traceId = patchStatus("INACTIVE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"))
            .andReturn().getResponse().getHeader("X-Trace-Id");
        patchStatus("INACTIVE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        assertThat(audits()).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT old_status, new_status, actor_id, trace_id FROM dbo.Room_Status_Audit"))
            .containsEntry("old_status", "ACTIVE").containsEntry("new_status", "INACTIVE")
            .containsEntry("actor_id", "ST000001").containsEntry("trace_id", traceId);
        patchStatus("ACTIVE").andExpect(status().isOk());
        assertThat(audits()).isEqualTo(2);
    }

    @Test void upcomingShowtime_inAnySalesStatus_blocks409_andChangesNothing() throws Exception {
        for (String sales : List.of("DRAFT", "OPEN", "CLOSED")) {
            jdbc.update("DELETE FROM dbo.Showtime");
            showtime("UP", now().plusDays(1), LocalTime.of(23, 0), sales);
            patchStatus("INACTIVE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_HAS_UPCOMING_SHOWTIMES")).andExpect(jsonPath("$.traceId").isNotEmpty());
        }
        assertThat(roomStatus()).isEqualTo("ACTIVE");
        assertThat(audits()).isZero();
        assertThat(jdbc.queryForObject("SELECT sales_status FROM dbo.Showtime WHERE showtime_id = 'UP'", String.class)).isEqualTo("CLOSED");
    }

    @Test void showtimeMoreThanOneMonthAhead_doesNotBlock() throws Exception {
        showtime("FAR", now().plusMonths(3), LocalTime.of(23, 0), "DRAFT");
        patchStatus("INACTIVE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test void showtimeWithinOneMonth_blocks() throws Exception {
        showtime("NEAR", now().plusDays(15), LocalTime.of(20, 0), "DRAFT");
        patchStatus("INACTIVE").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ROOM_HAS_UPCOMING_SHOWTIMES"));
    }

    @Test void endedShowtimeTodayDoesNotBlock_runningOneDoes() throws Exception {
        LocalDateTime now = now();
        org.junit.jupiter.api.Assumptions.assumeTrue(now.getHour() >= 4 && now.getHour() <= 21, "needs both fixtures on today's date");
        showtime("ENDED", now.minusHours(3), now.minusHours(1).toLocalTime(), "OPEN");
        patchStatus("INACTIVE").andExpect(status().isOk());
        patchStatus("ACTIVE").andExpect(status().isOk());
        showtime("RUNNING", now.minusHours(1), now.plusHours(1).toLocalTime(), "OPEN");
        patchStatus("INACTIVE").andExpect(status().isConflict());
    }

    @Test void showtimeFromYesterdayStillRunningPastMidnight_blocks() throws Exception {
        LocalDateTime now = now();
        org.junit.jupiter.api.Assumptions.assumeTrue(now.getHour() < 22, "end time must stay on today's date");
        jdbc.update("INSERT INTO dbo.Showtime (showtime_id, movie_id, room_id, show_date, start_time, end_time, sales_status) VALUES ('NIGHT', ?, ?, ?, '23:30', ?, 'CLOSED')",
            MOVIE, room, now.toLocalDate().minusDays(1), now.plusHours(1).toLocalTime());
        patchStatus("INACTIVE").andExpect(status().isConflict());
    }

    @Test void inactiveRoom_cannotBeScheduledOrOpened() throws Exception {
        String id = showtimes.createShowtime(request(now().plusDays(2).toLocalDate(), LocalTime.of(10, 0))).showtimeId();
        jdbc.update("UPDATE dbo.Room SET status = 'INACTIVE' WHERE room_id = ?", room);

        CinemaException create = (CinemaException) catchThrowable(() ->
            showtimes.createShowtime(request(now().plusDays(3).toLocalDate(), LocalTime.of(10, 0))));
        assertThat(create.code()).isEqualTo("ROOM_INACTIVE");
        CinemaException open = (CinemaException) catchThrowable(() -> showtimes.openShowtimeSales(id));
        assertThat(open.code()).isEqualTo("ROOM_INACTIVE");
        CinemaException move = (CinemaException) catchThrowable(() ->
            showtimes.updateShowtime(id, request(now().plusDays(4).toLocalDate(), LocalTime.of(10, 0))));
        assertThat(move.code()).isEqualTo("ROOM_INACTIVE");
    }

    @Test void statusChangeRacingWithScheduling_neverLeavesAShowtimeInAnInactiveRoom() throws Exception {
        for (int round = 0; round < 8; round++) {
            rooms();
            LocalDate day = now().plusDays(5).toLocalDate();
            var start = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            try {
                Future<Integer> patch = pool.submit(() -> { start.await(); return patchStatus("INACTIVE").andReturn().getResponse().getStatus(); });
                Future<String> create = pool.submit(() -> {
                    start.await();
                    try { showtimes.createShowtime(request(day, LocalTime.of(10, 0))); return "CREATED"; }
                    catch (CinemaException e) { return e.code(); }
                });
                start.countDown();
                int patchResult = patch.get(60, TimeUnit.SECONDS);
                String createResult = create.get(60, TimeUnit.SECONDS);
                List<Object> outcome = new ArrayList<>(List.of(patchResult, createResult));
                // Exactly one side wins: either the room went INACTIVE first, or the showtime was placed first.
                assertThat(outcome).isIn(List.of(200, "ROOM_INACTIVE"), List.of(409, "CREATED"));
            } finally { pool.shutdownNow(); }
            int showtimesInRoom = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime WHERE room_id = ?", Integer.class, room);
            assertThat(showtimesInRoom).isEqualTo(roomStatus().equals("INACTIVE") ? 0 : 1);
        }
    }

    private ShowtimeRequestDTO request(LocalDate day, LocalTime start) {
        return new ShowtimeRequestDTO(MOVIE, String.valueOf(room), day, start);
    }

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        return org.assertj.core.api.Assertions.catchThrowable(call);
    }
}
