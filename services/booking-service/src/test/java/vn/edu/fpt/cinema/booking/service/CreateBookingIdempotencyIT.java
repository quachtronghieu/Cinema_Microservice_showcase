package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.fpt.cinema.booking.support.BookingTestData.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest.ConcessionItem;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class CreateBookingIdempotencyIT extends SqlServerIT {
    static final String BODY = """
        {"holdId":"H1","showtimeId":"SW1","showtimeSeatIds":["SS1","SS2"],
         "concessions":[{"concessionId":"DR000002","quantity":2}],"voucherCode":null}""";

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BookingService bookings;
    @Autowired ObjectMapper json;
    @MockitoBean CatalogClient catalog;

    @BeforeEach void holdOfTwoSeats() {
        DbReset.clean(jdbc);
        showtimeWithFourSeats(jdbc);
        hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS1", "SS2");
        when(catalog.concession(any(), eq("DR000002")))
            .thenReturn(new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true));
    }

    private ResultActions create(String customerId, String key, String body) throws Exception {
        var request = post("/api/v1/customer/bookings").header("Authorization", TestJwt.customer(customerId))
            .contentType("application/json").content(body);
        if (key != null) request.header("Idempotency-Key", key);
        return mvc.perform(request);
    }

    private String bookingId(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).get("bookingId").asText();
    }

    @Test void sameKeySamePayload_secondCall200_sameBookingId_oneBookingRow() throws Exception {
        String first = bookingId(create("CS1", "key-1", BODY).andExpect(status().isCreated()));
        String second = bookingId(create("CS1", "key-1", BODY).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.totalAmount").value(270000.00)));
        assertThat(second).isEqualTo(first);
        assertThat(count(jdbc, "Booking")).isEqualTo(1);
        assertThat(count(jdbc, "IdempotencyRecord")).isEqualTo(1);
    }

    @Test void sameKeyDifferentPayload_409_IDEMPOTENCY_KEY_REUSED() throws Exception {
        create("CS1", "key-1", BODY).andExpect(status().isCreated());
        create("CS1", "key-1", BODY.replace("\"quantity\":2", "\"quantity\":3"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(count(jdbc, "Booking")).isEqualTo(1);
    }

    @Test void twoParallelSameKey_oneBookingRow() throws Exception {
        var request = new CustomerBookingRequest("H1", "SW1", List.of("SS1", "SS2"), List.of(new ConcessionItem("DR000002", 2)), null);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<BookingService.CreateResult>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                calls.add(pool.submit(() -> { start.await(); return bookings.create("CS1", "same-key", request); }));
            }
            start.countDown();
            var a = calls.get(0).get(30, TimeUnit.SECONDS);
            var b = calls.get(1).get(30, TimeUnit.SECONDS);
            assertThat(a.body().bookingId()).isEqualTo(b.body().bookingId());
            assertThat(List.of(a.replayed(), b.replayed())).containsExactlyInAnyOrder(false, true);
        } finally { pool.shutdownNow(); }
        assertThat(count(jdbc, "Booking")).isEqualTo(1);
    }

    @Test void replayAfterNewApplicationContext_still200() throws Exception {
        // A record committed by an earlier process run: replay must come from the database, not memory.
        String hash = RequestHasher.hash("create-booking", json.readValue(BODY, CustomerBookingRequest.class));
        jdbc.update("""
            INSERT INTO dbo.IdempotencyRecord (actor_id, operation, idem_key, request_hash, resource_id, response_status, response_body, created_at, expires_at)
            VALUES ('CS1', 'create-booking', 'key-before-restart', ?, 'B-OLD', 201,
                    '{"bookingId":"B-OLD","status":"PENDING_PAYMENT","totalAmount":270000.00,"expiresAt":"2026-11-04T03:05:00Z"}', ?, ?)""",
            hash, utc(Instant.now().minusSeconds(60)), utc(Instant.now().plusSeconds(86_000)));
        create("CS1", "key-before-restart", BODY).andExpect(status().isOk()).andExpect(jsonPath("$.bookingId").value("B-OLD"));
        assertThat(count(jdbc, "Booking")).isZero();
    }

    @Test void sameKeyByAnotherCustomer_isIndependent() throws Exception {
        hold(jdbc, "H2", "CS2", Instant.now().plusSeconds(300), "SS3");
        create("CS1", "shared-key", BODY).andExpect(status().isCreated());
        create("CS2", "shared-key", BODY.replace("H1", "H2").replace("[\"SS1\",\"SS2\"]", "[\"SS3\"]")).andExpect(status().isCreated());
        assertThat(count(jdbc, "Booking")).isEqualTo(2);
    }

    @Test void missingKey_400_IDEMPOTENCY_KEY_INVALID() throws Exception {
        create("CS1", null, BODY).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_INVALID"));
        assertThat(count(jdbc, "Booking")).isZero();
    }

    @Test void keyWithSpaceOrTooLong_400() throws Exception {
        create("CS1", "has space", BODY).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_INVALID"));
        create("CS1", "k".repeat(129), BODY).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_INVALID"));
    }
}
