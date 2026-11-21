package vn.edu.fpt.cinema.booking.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import vn.edu.fpt.cinema.booking.service.BookingService;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class BookingQueryIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BookingService bookings;
    @MockitoBean CatalogClient catalog;

    String bookingId;

    @BeforeEach void bookingOfCs1() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        when(catalog.concession(any(), eq("DR000002")))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
        bookingId = bookings.create("CS1", "key-1", new CustomerBookingRequest("H1", "SW1", List.of("SS2", "SS1"),
            List.of(new ConcessionItem("DR000002", 2)), null)).body().bookingId();
    }

    /** Detail shows purchased tickets only; payment (plan Task 7) will set PAID, tests set it directly. */
    private void markPaid(String id) {
        jdbc.update("UPDATE dbo.Booking SET status = 'PAID' WHERE booking_id = ?", id);
    }

    @Test void detail_own_200_usesSnapshotPrices_evenIfCatalogPriceChanged() throws Exception {
        markPaid(bookingId);
        when(catalog.concession(any(), eq("DR000002")))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("99000.00"), true));
        mvc.perform(get("/api/v1/customer/bookings/" + bookingId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(bookingId))
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.movieTitle").value("Dune"))
            .andExpect(jsonPath("$.roomName").value("Room 1"))
            .andExpect(jsonPath("$.showDate").value("2026-12-01"))
            .andExpect(jsonPath("$.startTime").value("19:00:00"))
            .andExpect(jsonPath("$.seats.length()").value(2))
            .andExpect(jsonPath("$.seats[0].label").value("A1"))
            .andExpect(jsonPath("$.seats[0].seatType").value("STANDARD"))
            .andExpect(jsonPath("$.seats[0].unitPrice").value(80000.00))
            .andExpect(jsonPath("$.seats[1].label").value("A2"))
            .andExpect(jsonPath("$.seats[1].seatType").value("VIP"))
            .andExpect(jsonPath("$.concessions[0].name").value("Coca Cola"))
            .andExpect(jsonPath("$.concessions[0].quantity").value(2))
            .andExpect(jsonPath("$.concessions[0].unitPrice").value(45000.00))
            .andExpect(jsonPath("$.concessions[0].lineTotal").value(90000.00))
            .andExpect(jsonPath("$.voucherCode").doesNotExist())
            .andExpect(jsonPath("$.discountAmount").value(0.00))
            .andExpect(jsonPath("$.totalAmount").value(270000.00))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test void detail_otherCustomer_403() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings/" + bookingId).header("Authorization", TestJwt.customer("CS2")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNED"));
    }

    @Test void detail_unknown_404() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings/no-such-booking").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    @Test void detail_unpaidBooking_404_BOOKING_NOT_PAID() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings/" + bookingId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOKING_NOT_PAID"));
    }

    @Test void detail_cancelledBooking_404_BOOKING_NOT_PAID() throws Exception {
        jdbc.update("UPDATE dbo.Booking SET status = 'CANCELLED' WHERE booking_id = ?", bookingId);
        mvc.perform(get("/api/v1/customer/bookings/" + bookingId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOKING_NOT_PAID"));
    }

    @Test void detail_showsVoucherCodeWhenUsed() throws Exception {
        jdbc.update("INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status) VALUES ('VC1', 'FPT', N'FPT', 'PERCENTAGE', 10, 5, 0, 0, 1)");
        hold(jdbc, "H2", "CS1", Instant.now().plusSeconds(300), "SS3");
        String withVoucher = bookings.create("CS1", "key-2", new CustomerBookingRequest("H2", "SW1", List.of("SS3"), List.of(), "FPT"))
            .body().bookingId();
        markPaid(withVoucher);
        mvc.perform(get("/api/v1/customer/bookings/" + withVoucher).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.voucherCode").value("FPT"))
            .andExpect(jsonPath("$.discountAmount").value(8000.00))
            .andExpect(jsonPath("$.totalAmount").value(72000.00));
    }

    /** One more paid booking for the customer from a fresh hold on the given seats, created at the given instant. */
    private String paidBooking(String customerId, String holdId, Instant createdAt, String... seats) {
        hold(jdbc, holdId, customerId, Instant.now().plusSeconds(300), seats);
        String id = bookings.create(customerId, holdId + "-key",
            new CustomerBookingRequest(holdId, "SW1", List.of(seats), List.of(), null)).body().bookingId();
        markPaid(id);
        jdbc.update("UPDATE dbo.Booking SET created_at = ? WHERE booking_id = ?", utc(createdAt), id);
        return id;
    }

    @Test void list_returnsOnlyCallersPaidBookings() throws Exception {
        String paid = paidBooking("CS1", "H2", Instant.now(), "SS3");
        paidBooking("CS2", "H3", Instant.now(), "SS4");
        // bookingId from @BeforeEach is CS1's but still PENDING_PAYMENT: not a purchased ticket.
        mvc.perform(get("/api/v1/customer/bookings").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].bookingId").value(paid))
            .andExpect(jsonPath("$.items[0].status").value("PAID"))
            .andExpect(jsonPath("$.items[0].movieTitle").value("Dune"))
            .andExpect(jsonPath("$.items[0].startTime").value("19:00:00"))
            .andExpect(jsonPath("$.items[0].seatLabels[0]").value("A3"))
            .andExpect(jsonPath("$.items[0].totalAmount").value(80000.00));
    }

    @Test void list_newestFirst_withPaging() throws Exception {
        String older = paidBooking("CS1", "H2", Instant.now().minusSeconds(3600), "SS3");
        String newer = paidBooking("CS1", "H3", Instant.now(), "SS4");
        mvc.perform(get("/api/v1/customer/bookings?page=0&size=1").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.items[0].bookingId").value(newer))
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/customer/bookings?page=1&size=1").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.items[0].bookingId").value(older));
    }

    @Test void list_size101_400() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings?size=101").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/customer/bookings?page=-1").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void seatLabels_inSeatOrder_A9BeforeA10_inListAndDetail() throws Exception {
        for (int n : new int[] {9, 10}) {
            jdbc.update("""
                INSERT INTO dbo.Showtime_Seat (showtime_seat_id, showtime_id, template_seat_id, row_label, seat_number, seat_type, unit_price, status)
                VALUES (?, 'SW1', ?, 'A', ?, 'STANDARD', 80000, 'AVAILABLE')""", "SS" + n, "TS" + n, n);
        }
        String id = paidBooking("CS1", "H2", Instant.now(), "SS10", "SS9");
        mvc.perform(get("/api/v1/customer/bookings").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.items[0].seatLabels[0]").value("A9"))
            .andExpect(jsonPath("$.items[0].seatLabels[1]").value("A10"));
        mvc.perform(get("/api/v1/customer/bookings/" + id).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.seats[0].label").value("A9"))
            .andExpect(jsonPath("$.seats[1].label").value("A10"));
    }
}
