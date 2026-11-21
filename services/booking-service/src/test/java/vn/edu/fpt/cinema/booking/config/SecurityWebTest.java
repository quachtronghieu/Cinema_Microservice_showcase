package vn.edu.fpt.cinema.booking.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.booking.service.BookingQueryService;
import vn.edu.fpt.cinema.booking.service.BookingService;
import vn.edu.fpt.cinema.booking.service.AdminVoucherService;
import vn.edu.fpt.cinema.booking.service.InternalSalesService;
import vn.edu.fpt.cinema.booking.service.PricingService;
import vn.edu.fpt.cinema.booking.service.SeatHoldService;
import vn.edu.fpt.cinema.booking.service.TicketCheckInService;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@WebMvcTest(properties = {
    "booking.security.issuer=" + TestJwt.ISSUER, "booking.security.jwk-set-uri=https://issuer.test/jwks",
    "booking.security.audience=" + TestJwt.AUDIENCE})
@Import({SecurityConfiguration.class, TestJwt.Keys.class})
class SecurityWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean PricingService pricing;
    @MockitoBean BookingService bookings;
    @MockitoBean AdminVoucherService vouchers;
    @MockitoBean BookingQueryService queries;
    @MockitoBean TicketCheckInService checkIns;
    @MockitoBean SeatHoldService seatHolds;
    @MockitoBean InternalSalesService internalSales;

    @Test void noToken_401_withCommonErrorJson() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test void guestCanReadAvailability_butOnlyThatShowtimePathIsPublic() throws Exception {
        mvc.perform(get("/api/v1/showtimes/SW1/seat-availability")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/showtimes/SW1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/showtimes/SW1/seat-availability")).andExpect(status().isUnauthorized());
    }

    @Test void guestCannotHoldSeats_401() throws Exception {
        mvc.perform(post("/api/v1/customer/seat-holds").contentType("application/json")
                .content("{\"showtimeId\":\"SW1\",\"showtimeSeatIds\":[\"SS1\"]}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test void adminRouteWithUserToken_403() throws Exception {
        mvc.perform(get("/api/v1/admin/tickets/T1").header("Authorization", TestJwt.customer("CS000001")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test void unknownCustomerPathWithValidToken_404_notInternalError() throws Exception {
        mvc.perform(get("/api/v1/customer/no-such-endpoint").header("Authorization", TestJwt.customer("CS000001")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test void unsupportedHttpMethod_405_notInternalError() throws Exception {
        mvc.perform(put("/api/v1/customer/bookings").header("Authorization", TestJwt.customer("CS000001")))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test void demoPaymentIsOffUnlessEnabled_404() throws Exception {
        mvc.perform(post("/api/v1/customer/bookings/B1/demo-payment").header("Authorization", TestJwt.customer("CS000001"))
                .contentType("application/json").content("{\"outcome\":\"SUCCESS\"}"))
            .andExpect(status().isNotFound());
    }

    @Test void traceIdFromGatewayIsReusedInResponse() throws Exception {
        mvc.perform(get("/api/v1/customer/bookings").header("X-Trace-Id", "gw-trace-123"))
            .andExpect(header().string("X-Trace-Id", "gw-trace-123"))
            .andExpect(jsonPath("$.traceId").value("gw-trace-123"));
    }
}
