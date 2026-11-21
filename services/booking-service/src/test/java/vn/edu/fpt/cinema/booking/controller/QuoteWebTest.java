package vn.edu.fpt.cinema.booking.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.QuoteFixtures.*;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.config.SecurityConfiguration;
import vn.edu.fpt.cinema.booking.entity.SeatHold;
import vn.edu.fpt.cinema.booking.repository.*;
import vn.edu.fpt.cinema.booking.service.PricingService;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@WebMvcTest(controllers = QuoteController.class, properties = {
    "booking.security.issuer=" + TestJwt.ISSUER, "booking.security.jwk-set-uri=https://issuer.test/jwks",
    "booking.security.audience=" + TestJwt.AUDIENCE})
@Import({SecurityConfiguration.class, TestJwt.Keys.class, PricingService.class, QuoteWebTest.FixedClock.class})
class QuoteWebTest {
    static final String URL = "/api/v1/customer/bookings/preview";
    static final String BODY = """
        {"holdId":"H1","showtimeId":"SW1","showtimeSeatIds":["SS1","SS2","SS3"],
         "concessions":[{"concessionId":"DR000002","quantity":2}],"voucherCode":null}""";

    @TestConfiguration static class FixedClock { @Bean Clock clock() { return CLOCK; } }

    @Autowired MockMvc mvc;
    @MockitoBean SeatHoldRepository holds;
    @MockitoBean ShowtimeSeatRepository seats;
    @MockitoBean SalesSnapshotRepository showtimes;
    @MockitoBean VoucherRepository vouchers;
    @MockitoBean CatalogClient catalog;

    @BeforeEach void heldSeatsAndCoca() {
        when(holds.findById("H1")).thenReturn(Optional.of(activeHold()));
        when(showtimes.findById("SW1")).thenReturn(Optional.of(openShowtime()));
        when(seats.findByHoldId("H1")).thenReturn(heldSeats());
        when(catalog.concession(any(), eq("DR000002"))).thenReturn(coca());
    }

    private org.springframework.test.web.servlet.ResultActions preview(String customerId, String body) throws Exception {
        return mvc.perform(post(URL).header("Authorization", TestJwt.customer(customerId))
            .contentType("application/json").content(body));
    }

    @Test void validPreview_200_pricedForTokenCustomer_andForwardsBearerToCatalog() throws Exception {
        preview(CUSTOMER, BODY).andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketItems.length()").value(3))
            .andExpect(jsonPath("$.ticketItems[0].itemType").value("SEAT"))
            .andExpect(jsonPath("$.ticketItems[0].name").value("A1"))
            .andExpect(jsonPath("$.concessionItems[0].itemType").value("CONCESSION"))
            .andExpect(jsonPath("$.concessionItems[0].lineTotal").value(90000.00))
            .andExpect(jsonPath("$.subtotal").value(350000.00))
            .andExpect(jsonPath("$.total").value(350000.00))
            .andExpect(jsonPath("$.voucher").doesNotExist());
        verify(catalog).concession(startsWith("Bearer "), eq("DR000002"));
    }

    @Test void holdOfAnotherCustomer_403() throws Exception {
        preview("CS-OTHER", BODY).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("HOLD_NOT_OWNED"));
    }

    @Test void expiredHold_409_HOLD_EXPIRED() throws Exception {
        when(holds.findById("H1")).thenReturn(Optional.of(expiredHold()));
        preview(CUSTOMER, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOLD_EXPIRED"));
    }

    @Test void consumedHold_409_HOLD_EXPIRED() throws Exception {
        when(holds.findById("H1")).thenReturn(Optional.of(new SeatHold("H1", CUSTOMER, "SW1", "CONSUMED", NOW.plusSeconds(60))));
        preview(CUSTOMER, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOLD_EXPIRED"));
    }

    @Test void inactiveConcession_409_CONCESSION_UNAVAILABLE() throws Exception {
        var request = Request.create(Request.HttpMethod.GET, "/api/v1/concessions/DR000002", Map.of(), null, StandardCharsets.UTF_8, null);
        when(catalog.concession(any(), eq("DR000002"))).thenThrow(new FeignException.NotFound("not found", request, null, Map.of()));
        preview(CUSTOMER, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONCESSION_UNAVAILABLE"));
    }

    @Test void catalogDown_503_DEPENDENCY_UNAVAILABLE() throws Exception {
        var request = Request.create(Request.HttpMethod.GET, "/api/v1/concessions/DR000002", Map.of(), null, StandardCharsets.UTF_8, null);
        when(catalog.concession(any(), eq("DR000002"))).thenThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.GET, (Long) null, request));
        preview(CUSTOMER, BODY).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test void nineSeats_400_withFieldError() throws Exception {
        String body = BODY.replace("[\"SS1\",\"SS2\",\"SS3\"]", "[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\"]");
        preview(CUSTOMER, body).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.showtimeSeatIds").exists());
    }

    @Test void adminTokenWithoutCustomerAccount_403_CUSTOMER_ACCOUNT_REQUIRED() throws Exception {
        mvc.perform(post(URL).header("Authorization", TestJwt.admin("ST1")).contentType("application/json").content(BODY))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CUSTOMER_ACCOUNT_REQUIRED"));
    }
}
