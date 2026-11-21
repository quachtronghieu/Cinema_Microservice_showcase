package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.BookingServiceApplication;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class SeatHoldIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SeatHoldExpiryJob expiryJob;
    @Autowired BookingService bookings;
    @Autowired Environment environment;

    @BeforeEach void showtime() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        jdbc.update("UPDATE dbo.Sales_Snapshot SET show_date = ?", LocalDate.now().plusDays(30));
    }

    private ResultActions post(String customerId, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/customer/seat-holds")
            .header("Authorization", TestJwt.customer(customerId)).contentType("application/json").content(body));
    }

    private static String body(String... seats) {
        return "{\"showtimeId\":\"SW1\",\"showtimeSeatIds\":[\"" + String.join("\",\"", seats) + "\"]}";
    }

    private ResultActions hold(String customerId, String... seats) throws Exception {
        return post(customerId, body(seats));
    }

    private String heldId(String customerId, String... seats) throws Exception {
        String response = hold(customerId, seats).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("holdId").asText();
    }

    private ResultActions release(String customerId, String holdId) throws Exception {
        return mvc.perform(delete("/api/v1/customer/seat-holds/" + holdId).header("Authorization", TestJwt.customer(customerId)));
    }

    private Map<String, Object> seat(String seatId) {
        return jdbc.queryForMap("SELECT status, hold_id, booking_id FROM dbo.Showtime_Seat WHERE showtime_seat_id = ?", seatId);
    }

    private String holdStatus(String holdId) {
        return jdbc.queryForObject("SELECT status FROM dbo.SeatHold WHERE hold_id = ?", String.class, holdId);
    }

    private int available() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime_Seat WHERE status = 'AVAILABLE'", Integer.class);
    }

    private void expire(String holdId) {
        jdbc.update("UPDATE dbo.SeatHold SET expires_at = ? WHERE hold_id = ?", utc(Instant.now().minusSeconds(1)), holdId);
    }

    @Test void events_areLoggedWithIds_andARejectedHoldIsNotReportedAsCreated(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        String released = heldId("CS1", "SS1", "SS2");
        hold("CS2", "SS1").andExpect(status().isConflict());
        release("CS1", released).andExpect(status().isNoContent());
        String expired = heldId("CS3", "SS3");
        expire(expired);
        expiryJob.expireOverdue();

        assertThat(output).contains("SEAT_HOLD_CREATED holdId=" + released + " showtimeId=SW1 customerId=CS1 seatIds=[SS1, SS2]");
        assertThat(output).contains("SEAT_HOLD_REJECTED holdId=null showtimeId=SW1 customerId=CS2 seatIds=[SS1] reason=SEAT_UNAVAILABLE");
        assertThat(output).contains("SEAT_HOLD_RELEASED holdId=" + released + " showtimeId=SW1");
        assertThat(output).contains("SEAT_HOLD_EXPIRED holdId=" + expired + " showtimeId=SW1");
        assertThat(output.toString()).doesNotContainPattern("SEAT_HOLD_CREATED .*customerId=CS2");
    }

    @Test void hold_ownsAllSeatsForFiveMinutes() throws Exception {
        String response = hold("CS1", "SS2", "SS1").andExpect(status().isCreated())
            .andExpect(jsonPath("$.showtimeId").value("SW1"))
            .andExpect(jsonPath("$.seatIds[0]").value("SS1")).andExpect(jsonPath("$.seatIds[1]").value("SS2"))
            .andReturn().getResponse().getContentAsString();
        var created = json.readTree(response);
        String holdId = created.get("holdId").asText();
        assertThat(Duration.between(Instant.parse(created.get("serverTime").asText()), Instant.parse(created.get("expiresAt").asText())))
            .isEqualTo(Duration.ofMinutes(5));
        assertThat(seat("SS1")).containsEntry("status", "HELD").containsEntry("hold_id", holdId);
        assertThat(seat("SS2")).containsEntry("status", "HELD").containsEntry("hold_id", holdId);
        assertThat(count(jdbc, "SeatHold_Item")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT customer_id FROM dbo.ActiveHold WHERE hold_id = ?", String.class, holdId)).isEqualTo("CS1");
    }

    @Test void invalidRequests_400_andChangeNothing() throws Exception {
        post("CS1", "{\"showtimeId\":\"SW1\",\"showtimeSeatIds\":[]}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.fieldErrors.showtimeSeatIds").isArray());
        hold("CS1", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        hold("CS1", "SS1", "SS1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SEAT_SELECTION"));
        hold("CS1", "SS1", "NOPE").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SEATS"));
        post("CS1", "{\"showtimeId\":\"SW1\",\"showtimeSeatIds\":[\"SS1\"],\"customerId\":\"CS2\"}").andExpect(status().isBadRequest());
        assertThat(available()).isEqualTo(4);
        assertThat(count(jdbc, "SeatHold")).isZero();
    }

    @Test void oneSeatTaken_409_namesIt_andHoldsNothing() throws Exception {
        heldId("CS1", "SS2");
        hold("CS2", "SS1", "SS2").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SEAT_UNAVAILABLE")).andExpect(jsonPath("$.seatIds[0]").value("SS2"))
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors").isMap());
        assertThat(seat("SS1")).containsEntry("status", "AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SeatHold WHERE customer_id = 'CS2'", Integer.class)).isZero();
    }

    @Test void manyCustomersRaceForOneSeat_exactlyOneWins_othersGet409() throws Exception {
        int contenders = 12;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(contenders);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < contenders; i++) {
                String customer = "CS" + i;
                results.add(pool.submit(() -> {
                    start.await();
                    return hold(customer, "SS1").andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) statuses.add(result.get(60, TimeUnit.SECONDS));
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        } finally { pool.shutdownNow(); }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SeatHold_Item WHERE showtime_seat_id = 'SS1'", Integer.class)).isEqualTo(1);
    }

    @Test void overlappingSelectionsInOppositeOrder_neverPartial_noDeadlock() throws Exception {
        for (int round = 0; round < 10; round++) {
            showtime();
            var start = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            try {
                Future<Integer> first = pool.submit(() -> { start.await(); return hold("CS1", "SS1", "SS2", "SS3").andReturn().getResponse().getStatus(); });
                Future<Integer> second = pool.submit(() -> { start.await(); return hold("CS2", "SS3", "SS2", "SS4").andReturn().getResponse().getStatus(); });
                start.countDown();
                assertThat(List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            } finally { pool.shutdownNow(); }
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime_Seat WHERE status = 'HELD'", Integer.class)).isEqualTo(3);
            assertThat(count(jdbc, "SeatHold")).isEqualTo(1);
        }
    }

    @Test void newHold_replacesPrevious_keepingOverlappingSeat() throws Exception {
        String first = heldId("CS1", "SS1", "SS2");
        String second = heldId("CS1", "SS2", "SS3");
        assertThat(holdStatus(first)).isEqualTo("RELEASED");
        assertThat(seat("SS1")).containsEntry("status", "AVAILABLE").containsEntry("hold_id", null);
        assertThat(seat("SS2")).containsEntry("hold_id", second);
        assertThat(seat("SS3")).containsEntry("hold_id", second);
        assertThat(count(jdbc, "ActiveHold")).isEqualTo(1);
    }

    @Test void failedReplacement_keepsPreviousHold() throws Exception {
        String first = heldId("CS1", "SS1");
        heldId("CS2", "SS2");
        hold("CS1", "SS2").andExpect(status().isConflict());
        assertThat(holdStatus(first)).isEqualTo("ACTIVE");
        assertThat(seat("SS1")).containsEntry("hold_id", first);
        assertThat(jdbc.queryForObject("SELECT hold_id FROM dbo.ActiveHold WHERE customer_id = 'CS1'", String.class)).isEqualTo(first);
    }

    @Test void sameCustomerConcurrently_hasOneActiveHold() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String seat : List.of("SS1", "SS2", "SS3", "SS4")) {
                results.add(pool.submit(() -> { start.await(); return hold("CS1", seat).andReturn().getResponse().getStatus(); }));
            }
            start.countDown();
            for (var result : results) assertThat(result.get(60, TimeUnit.SECONDS)).isEqualTo(201);
        } finally { pool.shutdownNow(); }
        assertThat(count(jdbc, "ActiveHold")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SeatHold WHERE status = 'ACTIVE'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime_Seat WHERE status = 'HELD'", Integer.class)).isEqualTo(1);
    }

    @Test void release_byOwner_204_repeatable_seatsCanBeHeldAgain() throws Exception {
        String holdId = heldId("CS1", "SS1");
        release("CS1", holdId).andExpect(status().isNoContent());
        release("CS1", holdId).andExpect(status().isNoContent());
        assertThat(holdStatus(holdId)).isEqualTo("RELEASED");
        assertThat(count(jdbc, "ActiveHold")).isZero();
        assertThat(available()).isEqualTo(4);
        heldId("CS2", "SS1");
    }

    @Test void release_otherCustomer403_unknown404_noToken401() throws Exception {
        String holdId = heldId("CS1", "SS1");
        release("CS2", holdId).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("HOLD_NOT_OWNED"));
        release("CS1", "no-such-hold").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));
        mvc.perform(delete("/api/v1/customer/seat-holds/" + holdId)).andExpect(status().isUnauthorized());
        assertThat(seat("SS1")).containsEntry("hold_id", holdId);
    }

    @Test void adminWithoutCustomerAccount_403() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/customer/seat-holds")
                .header("Authorization", TestJwt.admin("ST1")).contentType("application/json").content(body("SS1")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CUSTOMER_ACCOUNT_REQUIRED"));
    }

    @Test void holdThenBooking_thenReleaseOrExpiry_neverTouchesBookingSeats() throws Exception {
        String holdId = heldId("CS1", "SS1", "SS2");
        String bookingId = bookings.create("CS1", UUID.randomUUID().toString(),
            new CustomerBookingRequest(holdId, "SW1", List.of("SS1", "SS2"), List.of(), null)).body().bookingId();

        release("CS1", holdId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOLD_ALREADY_CONSUMED"));
        expiryJob.expireOverdue();

        assertThat(holdStatus(holdId)).isEqualTo("CONSUMED");
        assertThat(seat("SS1")).containsEntry("status", "HELD").containsEntry("booking_id", bookingId).containsEntry("hold_id", null);
        heldId("CS2", "SS3");
        hold("CS2", "SS1").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SEAT_UNAVAILABLE"));
    }

    @Test void expiryJob_releasesOnlyExpiredHolds() throws Exception {
        String expired = heldId("CS1", "SS1", "SS2");
        String current = heldId("CS2", "SS3");
        expire(expired);

        expiryJob.expireOverdue();

        assertThat(holdStatus(expired)).isEqualTo("EXPIRED");
        assertThat(holdStatus(current)).isEqualTo("ACTIVE");
        assertThat(seat("SS1")).containsEntry("status", "AVAILABLE").containsEntry("hold_id", null);
        assertThat(seat("SS3")).containsEntry("hold_id", current);
        assertThat(count(jdbc, "ActiveHold")).isEqualTo(1);
        heldId("CS3", "SS1");
    }

    @Test void expiredButNotYetProcessedHold_stillBlocksOthers_untilJobRuns() throws Exception {
        String expired = heldId("CS1", "SS1");
        expire(expired);
        hold("CS2", "SS1").andExpect(status().isConflict());
        expiryJob.expireOverdue();
        heldId("CS2", "SS1");
    }

    @Test void closedSales_409_startedShowtime_409_unknownShowtime_404() throws Exception {
        post("CS1", "{\"showtimeId\":\"NOPE\",\"showtimeSeatIds\":[\"SS1\"]}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SHOWTIME_NOT_FOUND"));
        jdbc.update("UPDATE dbo.Sales_Snapshot SET sales_status = 'CLOSED'");
        hold("CS1", "SS1").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SALES_CLOSED"));
        jdbc.update("UPDATE dbo.Sales_Snapshot SET sales_status = 'OPEN', show_date = ?", LocalDate.now().minusDays(1));
        hold("CS1", "SS1").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SHOWTIME_STARTED"));
        assertThat(available()).isEqualTo(4);
    }

    @Test void guestSeesAvailability_withoutAnyOwnerData() throws Exception {
        String holdId = heldId("CS1", "SS1");
        String response = mvc.perform(get("/api/v1/showtimes/SW1/seat-availability")).andExpect(status().isOk())
            .andExpect(jsonPath("$.showtimeId").value("SW1")).andExpect(jsonPath("$.salesStatus").value("OPEN"))
            .andExpect(jsonPath("$.serverTime").isNotEmpty())
            .andExpect(jsonPath("$.seats.length()").value(4))
            .andExpect(jsonPath("$.seats[0].showtimeSeatId").value("SS1")).andExpect(jsonPath("$.seats[0].status").value("HELD"))
            .andExpect(jsonPath("$.seats[0].templateSeatId").value("TS1")).andExpect(jsonPath("$.seats[0].rowLabel").value("A"))
            .andExpect(jsonPath("$.seats[1].status").value("AVAILABLE")).andExpect(jsonPath("$.seats[1].seatType").value("VIP"))
            .andExpect(jsonPath("$.seats[1].unitPrice").value(100000.0))
            .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(holdId).doesNotContain("CS1").doesNotContain("holdId").doesNotContain("customerId");
        mvc.perform(get("/api/v1/showtimes/NOPE/seat-availability")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SHOWTIME_NOT_FOUND"));
    }

    @Test void holdOnInstanceA_bookingOnInstanceB_survivesStartOfB() throws Exception {
        String holdId = heldId("CS1", "SS1");
        // Command-line arguments outrank application.yaml, which has no defaults for the database settings.
        String[] sameDatabase = {
            "--server.port=0",
            "--spring.datasource.url=" + environment.getRequiredProperty("spring.datasource.url"),
            "--spring.datasource.username=" + environment.getRequiredProperty("spring.datasource.username"),
            "--spring.datasource.password=" + environment.getRequiredProperty("spring.datasource.password"),
            "--spring.flyway.url=" + environment.getRequiredProperty("spring.flyway.url"),
            "--spring.flyway.user=" + environment.getRequiredProperty("spring.flyway.user"),
            "--spring.flyway.password=" + environment.getRequiredProperty("spring.flyway.password"),
            "--eureka.client.enabled=false",
            "--booking.security.jwk-set-uri=https://issuer.test/jwks",
            "--booking.seat-hold.expiry-delay=PT1H"};
        try (var instanceB = new SpringApplicationBuilder(BookingServiceApplication.class).run(sameDatabase)) {
            instanceB.getBean(SeatHoldExpiryJob.class).expireOverdue();
            assertThat(holdStatus(holdId)).isEqualTo("ACTIVE");
            assertThat(seat("SS1")).containsEntry("hold_id", holdId);

            String bookingId = instanceB.getBean(BookingService.class).create("CS1", UUID.randomUUID().toString(),
                new CustomerBookingRequest(holdId, "SW1", List.of("SS1"), List.of(), null)).body().bookingId();
            assertThat(seat("SS1")).containsEntry("booking_id", bookingId);
        }
    }
}
