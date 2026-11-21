package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import feign.Request;
import feign.RetryableException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest.ConcessionItem;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.support.BookingTestData;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class CreateBookingIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired BookingService bookings;
    @Autowired MockMvc mvc;
    @MockitoBean CatalogClient catalog;

    @BeforeEach void showtimeWithFourSeats() {
        DbReset.clean(jdbc);
        BookingTestData.showtimeWithFourSeats(jdbc);
        when(catalog.concession(any(), eq("DR000002")))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
    }

    private void hold(String holdId, String customerId, Instant expiresAt, String... seatIds) {
        BookingTestData.hold(jdbc, holdId, customerId, expiresAt, seatIds);
    }

    private void voucher(int usageLimit, int usedCount) {
        jdbc.update("INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status) VALUES ('VC1', 'FPT', N'FPT', 'PERCENTAGE', 10, ?, ?, 0, 1)",
            usageLimit, usedCount);
    }

    private static CustomerBookingRequest request(String holdId, List<String> seats, String voucher) {
        return new CustomerBookingRequest(holdId, "SW1", seats, List.of(new ConcessionItem("DR000002", 2)), voucher);
    }

    private int count(String table) { return BookingTestData.count(jdbc, table); }

    @Test void create_happyPath_seatsOwnedByBooking_holdConsumed_expiresIn5Minutes() throws Exception {
        hold("H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        mvc.perform(post("/api/v1/customer/bookings").header("Authorization", TestJwt.customer("CS1")).header("Idempotency-Key", "happy-path").contentType("application/json")
                .content("""
                    {"holdId":"H1","showtimeId":"SW1","showtimeSeatIds":["SS1","SS2"],
                     "concessions":[{"concessionId":"DR000002","quantity":2}],"voucherCode":null}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.totalAmount").value(270000.00))
            .andExpect(jsonPath("$.bookingId").isNotEmpty())
            .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        var booking = jdbc.queryForMap("SELECT * FROM dbo.Booking");
        String bookingId = (String) booking.get("booking_id");
        assertThat(booking).containsEntry("customer_id", "CS1").containsEntry("status", "PENDING_PAYMENT").containsEntry("channel", "CUSTOMER");
        assertThat((BigDecimal) booking.get("total_amount")).isEqualByComparingTo("270000");
        var created = ((Timestamp) booking.get("created_at")).toInstant();
        assertThat(Duration.between(created, ((Timestamp) booking.get("expires_at")).toInstant())).isEqualTo(Duration.ofMinutes(5));

        assertThat(jdbc.queryForList("SELECT status, hold_id, booking_id FROM dbo.Showtime_Seat WHERE showtime_seat_id IN ('SS1','SS2')"))
            .allSatisfy(seat -> assertThat(seat).containsEntry("status", "HELD").containsEntry("booking_id", bookingId).containsEntry("hold_id", null));
        assertThat(jdbc.queryForMap("SELECT status, booking_id FROM dbo.SeatHold WHERE hold_id = 'H1'"))
            .containsEntry("status", "CONSUMED").containsEntry("booking_id", bookingId);
        assertThat(count("ActiveHold")).isZero();
        assertThat(jdbc.queryForList("SELECT item_type, item_name, quantity, unit_price FROM dbo.Booking_Detail ORDER BY item_type DESC, item_name"))
            .extracting(row -> row.get("item_type") + " " + row.get("item_name") + " x" + row.get("quantity") + " @" + ((BigDecimal) row.get("unit_price")).stripTrailingZeros().toPlainString())
            .containsExactly("SEAT A1 x1 @80000", "SEAT A2 x1 @100000", "CONCESSION Coca Cola x2 @45000");
    }

    @Test void create_expiredHold_409_noBookingRow() {
        hold("H1", "CS1", Instant.now().minusSeconds(1), "SS1");
        assertThatThrownBy(() -> bookings.create("CS1", UUID.randomUUID().toString(), request("H1", List.of("SS1"), null)))
            .isInstanceOfSatisfying(BookingException.class, e -> assertThat(e.code()).isEqualTo("HOLD_EXPIRED"));
        assertThat(count("Booking")).isZero();
    }

    @Test void create_twoParallelRequestsSameHold_exactlyOneBooking() throws Exception {
        hold("H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        var outcomes = race(() -> bookings.create("CS1", UUID.randomUUID().toString(), request("H1", List.of("SS1", "SS2"), null)),
                            () -> bookings.create("CS1", UUID.randomUUID().toString(), request("H1", List.of("SS1", "SS2"), null)));
        assertThat(outcomes).containsExactlyInAnyOrder("OK", "HOLD_EXPIRED");
        assertThat(count("Booking")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT booking_id) FROM dbo.Showtime_Seat WHERE booking_id IS NOT NULL", Integer.class)).isEqualTo(1);
    }

    @Test void voucherLastQuota_twoParallelCustomers_oneDiscounted_other409() throws Exception {
        voucher(1, 0);
        hold("H1", "CS1", Instant.now().plusSeconds(300), "SS1");
        hold("H2", "CS2", Instant.now().plusSeconds(300), "SS3");
        var outcomes = race(() -> bookings.create("CS1", UUID.randomUUID().toString(), request("H1", List.of("SS1"), "FPT")),
                            () -> bookings.create("CS2", UUID.randomUUID().toString(), request("H2", List.of("SS3"), "FPT")));
        assertThat(outcomes).containsExactlyInAnyOrder("OK", "VOUCHER_EXHAUSTED");
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM dbo.Voucher WHERE voucher_id = 'VC1'", Integer.class)).isEqualTo(1);
        assertThat(count("Voucher_Reservation")).isEqualTo(1);
        assertThat(count("Booking")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT discount_amount FROM dbo.Booking", BigDecimal.class)).isEqualByComparingTo("17000");
    }

    @Test void catalogDown_503_noBookingRow() {
        hold("H1", "CS1", Instant.now().plusSeconds(300), "SS1");
        var req = Request.create(Request.HttpMethod.GET, "/api/v1/concessions/DR000002", Map.of(), null, StandardCharsets.UTF_8, null);
        when(catalog.concession(any(), eq("DR000002"))).thenThrow(new RetryableException(-1, "refused", Request.HttpMethod.GET, (Long) null, req));
        assertThatThrownBy(() -> bookings.create("CS1", UUID.randomUUID().toString(), request("H1", List.of("SS1"), null)))
            .isInstanceOfSatisfying(BookingException.class, e -> assertThat(e.code()).isEqualTo("DEPENDENCY_UNAVAILABLE"));
        assertThat(count("Booking")).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM dbo.SeatHold WHERE hold_id = 'H1'", String.class)).isEqualTo("ACTIVE");
    }

    /** Starts both calls at the same instant; returns "OK" or the BookingException code for each. */
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
