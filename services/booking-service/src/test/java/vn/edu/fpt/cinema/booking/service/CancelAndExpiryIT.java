package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
class CancelAndExpiryIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BookingService bookings;
    @Autowired BookingReleaseService release;
    @Autowired BookingExpiryJob expiryJob;
    @MockitoBean CatalogClient catalog;

    @BeforeEach void showtime() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        when(catalog.concession(any(), any()))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
    }

    private String book(String customerId, String holdId, String voucher, String... seats) {
        hold(jdbc, holdId, customerId, Instant.now().plusSeconds(300), seats);
        return bookings.create(customerId, UUID.randomUUID().toString(),
            new CustomerBookingRequest(holdId, "SW1", List.of(seats), List.of(), voucher)).body().bookingId();
    }

    private ResultActions cancel(String customerId, String bookingId) throws Exception {
        return mvc.perform(post("/api/v1/customer/bookings/" + bookingId + "/cancel").header("Authorization", TestJwt.customer(customerId)));
    }

    private String bookingStatus(String bookingId) {
        return jdbc.queryForObject("SELECT status FROM dbo.Booking WHERE booking_id = ?", String.class, bookingId);
    }

    private int reserved() {
        return jdbc.queryForObject("SELECT reserved_count FROM dbo.Voucher WHERE voucher_id = 'VC1'", Integer.class);
    }

    private void voucher() {
        jdbc.update("INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status) VALUES ('VC1', 'FPT', N'FPT', 'PERCENTAGE', 10, 5, 0, 0, 1)");
    }

    @Test void cancel_pending_seatsAvailable_andCanBeHeldAgain() throws Exception {
        String id = book("CS1", "H1", null, "SS1", "SS2");
        cancel("CS1", id).andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(id)).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(jdbc.queryForList("SELECT status, booking_id, hold_id FROM dbo.Showtime_Seat WHERE showtime_seat_id IN ('SS1','SS2')"))
            .allSatisfy(seat -> assertThat(seat).containsEntry("status", "AVAILABLE").containsEntry("booking_id", null).containsEntry("hold_id", null));
        // The released seats can be bought again by someone else.
        String again = book("CS2", "H2", null, "SS1", "SS2");
        assertThat(bookingStatus(again)).isEqualTo("PENDING_PAYMENT");
    }

    @Test void cancelTwice_voucherReservedCountDecrementedOnce() throws Exception {
        voucher();
        String id = book("CS1", "H1", "FPT", "SS1");
        assertThat(reserved()).isEqualTo(1);
        cancel("CS1", id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel("CS1", id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(reserved()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM dbo.Voucher_Reservation WHERE booking_id = ?", String.class, id)).isEqualTo("RELEASED");
    }

    @Test void cancel_paid_409() throws Exception {
        String id = book("CS1", "H1", null, "SS1");
        jdbc.update("UPDATE dbo.Booking SET status = 'PAID' WHERE booking_id = ?", id);
        cancel("CS1", id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_NOT_CANCELLABLE"));
        assertThat(jdbc.queryForObject("SELECT booking_id FROM dbo.Showtime_Seat WHERE showtime_seat_id = 'SS1'", String.class)).isEqualTo(id);
    }

    @Test void cancel_otherCustomersBooking_403() throws Exception {
        String id = book("CS1", "H1", null, "SS1");
        cancel("CS2", id).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNED"));
        assertThat(bookingStatus(id)).isEqualTo("PENDING_PAYMENT");
    }

    @Test void expiryJob_expiresOnlyOverdue_releasesOnlyItsOwnSeats() {
        voucher();
        String overdue = book("CS1", "H1", "FPT", "SS1");
        String current = book("CS2", "H2", null, "SS2");
        jdbc.update("UPDATE dbo.Booking SET expires_at = ? WHERE booking_id = ?", utc(Instant.now().minusSeconds(1)), overdue);

        expiryJob.expireOverdue();

        assertThat(bookingStatus(overdue)).isEqualTo("EXPIRED");
        assertThat(bookingStatus(current)).isEqualTo("PENDING_PAYMENT");
        assertThat(jdbc.queryForMap("SELECT status, booking_id FROM dbo.Showtime_Seat WHERE showtime_seat_id = 'SS1'"))
            .containsEntry("status", "AVAILABLE").containsEntry("booking_id", null);
        assertThat(jdbc.queryForObject("SELECT booking_id FROM dbo.Showtime_Seat WHERE showtime_seat_id = 'SS2'", String.class)).isEqualTo(current);
        assertThat(reserved()).isZero();
    }

    @Test void cancelAndExpiryAtTheSameTime_oneFinalState_releasedOnce() throws Exception {
        voucher();
        String id = book("CS1", "H1", "FPT", "SS1", "SS2");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> cancelled = pool.submit(() -> { start.await(); return release.release(id, "CANCELLED"); });
            Future<Boolean> expired = pool.submit(() -> { start.await(); return release.release(id, "EXPIRED"); });
            start.countDown();
            assertThat(List.of(cancelled.get(30, TimeUnit.SECONDS), expired.get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); }
        assertThat(bookingStatus(id)).isIn("CANCELLED", "EXPIRED");
        assertThat(reserved()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Showtime_Seat WHERE status = 'AVAILABLE'", Integer.class)).isEqualTo(4);
    }
}
