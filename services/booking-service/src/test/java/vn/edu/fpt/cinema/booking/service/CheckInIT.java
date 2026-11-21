package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class CheckInIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BookingService bookings;
    @Autowired PaymentService payments;
    @MockitoBean CatalogClient catalog;

    String bookingId;
    String ticketId;

    @BeforeEach void paidTicketOfCs1() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        jdbc.update("UPDATE dbo.Sales_Snapshot SET show_date = ? WHERE showtime_id = 'SW1'", LocalDate.now().plusDays(30));
        hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        bookingId = bookings.create("CS1", "key-1",
            new CustomerBookingRequest("H1", "SW1", List.of("SS1", "SS2"), List.of(), null)).body().bookingId();
        ticketId = payments.pay("CS1", bookingId, "SUCCESS").ticketId();
    }

    private ResultActions checkIn(String authorization) throws Exception {
        return mvc.perform(post("/api/v1/admin/tickets/" + ticketId + "/check-in").header("Authorization", authorization));
    }

    private Map<String, Object> ticketRow() {
        return jdbc.queryForMap("SELECT checked_in, checked_in_at, checked_in_by FROM dbo.Ticket WHERE ticket_id = ?", ticketId);
    }

    @Test void admin_checksIn_200_andCustomerSeesCheckedIn() throws Exception {
        checkIn(TestJwt.admin("ST1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketId").value(ticketId))
            .andExpect(jsonPath("$.checkedInAt").isNotEmpty())
            .andExpect(jsonPath("$.checkedInBy").value("ST1"));
        assertThat(ticketRow()).containsEntry("checked_in", true).containsEntry("checked_in_by", "ST1");
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.status").value("CHECKED_IN"))
            .andExpect(jsonPath("$.checkedIn").value(true));
    }

    @Test void secondCheckIn_409_firstRecordKept() throws Exception {
        checkIn(TestJwt.admin("ST1")).andExpect(status().isOk());
        var first = ticketRow();
        checkIn(TestJwt.admin("ST2")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TICKET_ALREADY_CHECKED_IN"));
        assertThat(ticketRow()).isEqualTo(first);
    }

    @Test void checkIn_twiceInParallel_oneSuccess_one409() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String staff : List.of("ST1", "ST2")) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return checkIn(TestJwt.admin(staff)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var future : futures) statuses.add(future.get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally { pool.shutdownNow(); }
        assertThat(ticketRow()).containsEntry("checked_in", true);
    }

    @Test void userToken_403() throws Exception {
        checkIn(TestJwt.customer("CS1")).andExpect(status().isForbidden());
        assertThat(ticketRow()).containsEntry("checked_in", false);
    }

    @Test void adminTokenWithoutStaffId_403() throws Exception {
        checkIn(TestJwt.bearer("ADMIN", Map.of())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("STAFF_ACCOUNT_REQUIRED"));
    }

    @Test void showtimeAlreadyStarted_409_TICKET_EXPIRED() throws Exception {
        jdbc.update("UPDATE dbo.Sales_Snapshot SET show_date = ? WHERE showtime_id = 'SW1'", LocalDate.now().minusDays(1));
        checkIn(TestJwt.admin("ST1")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TICKET_EXPIRED"));
        assertThat(ticketRow()).containsEntry("checked_in", false);
    }

    @Test void ticketOfCancelledBooking_409() throws Exception {
        jdbc.update("UPDATE dbo.Booking SET status = 'CANCELLED' WHERE booking_id = ?", bookingId);
        checkIn(TestJwt.admin("ST1")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TICKET_NOT_VALID"));
        assertThat(ticketRow()).containsEntry("checked_in", false);
    }

    @Test void unknownTicket_404() throws Exception {
        mvc.perform(post("/api/v1/admin/tickets/no-such-ticket/check-in").header("Authorization", TestJwt.admin("ST1")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
