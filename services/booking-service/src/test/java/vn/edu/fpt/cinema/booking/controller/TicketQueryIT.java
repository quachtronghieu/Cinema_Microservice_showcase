package vn.edu.fpt.cinema.booking.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import vn.edu.fpt.cinema.booking.service.PaymentService;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class TicketQueryIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BookingService bookings;
    @Autowired PaymentService payments;
    @MockitoBean CatalogClient catalog;

    String bookingId;
    LocalDate showDate = LocalDate.now().plusDays(30);

    @BeforeEach void pendingBookingOfCs1() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS2", "SS1");
        when(catalog.concession(any(), eq("DR000002")))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
        // A showtime in the future relative to the real clock, so the ticket is VALID whenever the suite runs.
        jdbc.update("UPDATE dbo.Sales_Snapshot SET show_date = ? WHERE showtime_id = 'SW1'", showDate);
        bookingId = bookings.create("CS1", "key-1", new CustomerBookingRequest("H1", "SW1", List.of("SS2", "SS1"),
            List.of(new ConcessionItem("DR000002", 2)), null)).body().bookingId();
    }

    private String paidTicketId() {
        return payments.pay("CS1", bookingId, "SUCCESS").ticketId();
    }

    @Test void tickets_onlyAfterPaid() throws Exception {
        mvc.perform(get("/api/v1/customer/tickets").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        String ticketId = paidTicketId();
        mvc.perform(get("/api/v1/customer/tickets").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].ticketId").value(ticketId));
        mvc.perform(get("/api/v1/customer/tickets").header("Authorization", TestJwt.customer("CS2")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void detail_hasFieldsTheFlutterTicketScreenReads() throws Exception {
        String ticketId = paidTicketId();
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketId").value(ticketId))
            .andExpect(jsonPath("$.bookingId").value(bookingId))
            .andExpect(jsonPath("$.movieTitle").value("Dune"))
            .andExpect(jsonPath("$.showDate").value(showDate.toString()))
            .andExpect(jsonPath("$.startTime").value("19:00:00"))
            .andExpect(jsonPath("$.roomId").value("1"))
            .andExpect(jsonPath("$.roomName").value("Room 1"))
            .andExpect(jsonPath("$.seatLabels[0]").value("A1"))
            .andExpect(jsonPath("$.seatLabels[1]").value("A2"))
            .andExpect(jsonPath("$.concessionItems[0].concessionId").value("DR000002"))
            .andExpect(jsonPath("$.concessionItems[0].name").value("Coca Cola"))
            .andExpect(jsonPath("$.concessionItems[0].quantity").value(2))
            .andExpect(jsonPath("$.concessionItems[0].unitPrice").value(45000.00))
            .andExpect(jsonPath("$.concessionItems[0].lineTotal").value(90000.00))
            .andExpect(jsonPath("$.totalAmount").value(270000.00))
            .andExpect(jsonPath("$.checkedIn").value(false))
            .andExpect(jsonPath("$.status").value("VALID"))
            .andExpect(jsonPath("$.checkedInAt").doesNotExist());
    }

    @Test void qrPayloadContainsServerTicketId() throws Exception {
        String ticketId = paidTicketId();
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.qrPayload").value("CINEMA-TICKET:" + ticketId));
    }

    @Test void checkedInTicket_showsCheckedInStatus() throws Exception {
        String ticketId = paidTicketId();
        jdbc.update("UPDATE dbo.Ticket SET checked_in = 1, checked_in_at = ?, checked_in_by = 'ST1' WHERE ticket_id = ?",
            utc(Instant.now()), ticketId);
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.checkedIn").value(true))
            .andExpect(jsonPath("$.status").value("CHECKED_IN"))
            .andExpect(jsonPath("$.checkedInAt").isNotEmpty());
    }

    @Test void showtimeAlreadyStarted_notCheckedIn_expired() throws Exception {
        String ticketId = paidTicketId();
        jdbc.update("UPDATE dbo.Sales_Snapshot SET show_date = ? WHERE showtime_id = 'SW1'", LocalDate.now().minusDays(1));
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS1")))
            .andExpect(jsonPath("$.status").value("EXPIRED"))
            .andExpect(jsonPath("$.checkedIn").value(false));
    }

    @Test void ticketOfAnotherCustomer_403() throws Exception {
        String ticketId = paidTicketId();
        mvc.perform(get("/api/v1/customer/tickets/" + ticketId).header("Authorization", TestJwt.customer("CS2")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TICKET_NOT_OWNED"));
    }

    @Test void unknownTicket_404() throws Exception {
        mvc.perform(get("/api/v1/customer/tickets/no-such-ticket").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
