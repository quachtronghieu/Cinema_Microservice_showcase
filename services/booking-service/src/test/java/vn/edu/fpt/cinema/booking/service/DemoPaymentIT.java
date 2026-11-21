package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
@TestPropertySource(properties = "booking.demo-payment.enabled=true")
class DemoPaymentIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BookingService bookings;
    @Autowired PaymentService payments;
    @MockitoBean CatalogClient catalog;

    String bookingId;

    @BeforeEach void pendingBookingWithVoucher() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        jdbc.update("INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status) VALUES ('VC1', 'FPT', N'FPT', 'PERCENTAGE', 10, 5, 0, 0, 1)");
        when(catalog.concession(any(), any()))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
        hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        bookingId = bookings.create("CS1", UUID.randomUUID().toString(),
            new CustomerBookingRequest("H1", "SW1", List.of("SS1", "SS2"), List.of(), "FPT")).body().bookingId();
    }

    private ResultActions pay(String customerId, String outcome) throws Exception {
        return mvc.perform(post("/api/v1/customer/bookings/" + bookingId + "/demo-payment")
            .header("Authorization", TestJwt.customer(customerId)).contentType("application/json")
            .content("{\"outcome\":\"" + outcome + "\"}"));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String bookingStatus() {
        return jdbc.queryForObject("SELECT status FROM dbo.Booking WHERE booking_id = ?", String.class, bookingId);
    }

    private List<String> seatStatuses() {
        return jdbc.queryForList("SELECT status FROM dbo.Showtime_Seat WHERE showtime_seat_id IN ('SS1','SS2')", String.class);
    }

    @Test void success_paidSoldAndExactlyOneTicket() throws Exception {
        var paid = body(pay("CS1", "SUCCESS").andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(bookingId))
            .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
            .andExpect(jsonPath("$.amount").value(162000.00))   // (80000 + 100000) - 10%
            .andExpect(jsonPath("$.paymentId").isNotEmpty())
            .andExpect(jsonPath("$.ticketId").isNotEmpty())
            .andExpect(jsonPath("$.message").isNotEmpty()));
        assertThat(bookingStatus()).isEqualTo("PAID");
        assertThat(seatStatuses()).containsOnly("SOLD");
        assertThat(count(jdbc, "Ticket")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ticket_id FROM dbo.Ticket", String.class)).isEqualTo(paid.get("ticketId").asText());
        assertThat(jdbc.queryForMap("SELECT status, amount FROM dbo.Payment"))
            .containsEntry("status", "SUCCEEDED").satisfies(p -> assertThat((BigDecimal) p.get("amount")).isEqualByComparingTo("162000"));
        assertThat(jdbc.queryForMap("SELECT used_count, reserved_count FROM dbo.Voucher WHERE voucher_id = 'VC1'"))
            .containsEntry("used_count", 1).containsEntry("reserved_count", 0);
        assertThat(jdbc.queryForObject("SELECT status FROM dbo.Voucher_Reservation", String.class)).isEqualTo("CONSUMED");
    }

    @Test void sameOutcomeTwice_oneTicket_secondCallReplays200() throws Exception {
        var first = body(pay("CS1", "SUCCESS").andExpect(status().isOk()));
        var second = body(pay("CS1", "SUCCESS").andExpect(status().isOk()));
        assertThat(second.get("ticketId").asText()).isEqualTo(first.get("ticketId").asText());
        assertThat(second.get("paymentId").asText()).isEqualTo(first.get("paymentId").asText());
        assertThat(count(jdbc, "Ticket")).isEqualTo(1);
        assertThat(count(jdbc, "Payment")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT used_count FROM dbo.Voucher WHERE voucher_id = 'VC1'", Integer.class)).isEqualTo(1);
    }

    @Test void failed_releasesSeats_thenSuccess409() throws Exception {
        pay("CS1", "FAILED").andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
            .andExpect(jsonPath("$.ticketId").doesNotExist());
        assertThat(bookingStatus()).isEqualTo("CANCELLED");
        assertThat(seatStatuses()).containsOnly("AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM dbo.Voucher WHERE voucher_id = 'VC1'", Integer.class)).isZero();
        pay("CS1", "SUCCESS").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_NOT_PAYABLE"));
        assertThat(count(jdbc, "Ticket")).isZero();
    }

    @Test void afterExpiresAt_409_noTicket() throws Exception {
        jdbc.update("UPDATE dbo.Booking SET expires_at = ? WHERE booking_id = ?", utc(Instant.now().minusSeconds(1)), bookingId);
        pay("CS1", "SUCCESS").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_NOT_PAYABLE"));
        assertThat(count(jdbc, "Ticket")).isZero();
        assertThat(count(jdbc, "Payment")).isZero();
    }

    @Test void otherCustomer_403() throws Exception {
        pay("CS2", "SUCCESS").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNED"));
        assertThat(bookingStatus()).isEqualTo("PENDING_PAYMENT");
    }

    @Test void invalidOutcome_400() throws Exception {
        pay("CS1", "MAYBE").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void detail_afterPayment_showsPaymentAndTicket() throws Exception {
        var paid = body(pay("CS1", "SUCCESS"));
        mvc.perform(get("/api/v1/customer/bookings/" + bookingId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.payment.paymentId").value(paid.get("paymentId").asText()))
            .andExpect(jsonPath("$.payment.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.payment.paidAt").isNotEmpty())
            .andExpect(jsonPath("$.ticketId").value(paid.get("ticketId").asText()));
    }

    @Test void paymentAndCancelInParallel_exactlyOneFinalState() throws Exception {
        var outcomes = race(() -> payments.pay("CS1", bookingId, "SUCCESS"), () -> bookings.cancel("CS1", bookingId));
        if ("PAID".equals(bookingStatus())) {
            assertThat(outcomes).containsExactly("OK", "BOOKING_NOT_CANCELLABLE");   // payment won
            assertThat(seatStatuses()).containsOnly("SOLD");
            assertThat(count(jdbc, "Ticket")).isEqualTo(1);
        } else {
            assertThat(outcomes).containsExactly("BOOKING_NOT_PAYABLE", "OK");      // cancel won
            assertThat(bookingStatus()).isEqualTo("CANCELLED");
            assertThat(seatStatuses()).containsOnly("AVAILABLE");
            assertThat(count(jdbc, "Ticket")).isZero();
        }
    }

    @Test void doubleClickParallelSuccess_oneTicketBothSucceed() throws Exception {
        var outcomes = race(() -> payments.pay("CS1", bookingId, "SUCCESS"), () -> payments.pay("CS1", bookingId, "SUCCESS"));
        assertThat(outcomes).containsOnly("OK");
        assertThat(count(jdbc, "Ticket")).isEqualTo(1);
        assertThat(count(jdbc, "Payment")).isEqualTo(1);
    }

    /** Runs both at the same instant; "OK" or the BookingException code for each. */
    private static List<String> race(Callable<?> first, Callable<?> second) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Callable<?> call : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try { call.call(); return "OK"; }
                    catch (BookingException e) { return e.code(); }
                }));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (var future : futures) outcomes.add(future.get(30, TimeUnit.SECONDS));
            return outcomes;
        } finally { pool.shutdownNow(); }
    }
}
