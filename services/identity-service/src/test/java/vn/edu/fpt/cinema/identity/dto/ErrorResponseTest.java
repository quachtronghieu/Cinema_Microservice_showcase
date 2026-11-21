package vn.edu.fpt.cinema.identity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ErrorResponseTest {
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @AfterEach
    void clear() {
        MDC.remove("traceId");
    }

    @Test
    void usesTheDistributedTraceId() {
        MDC.put("traceId", TRACE_ID);
        assertThat(new ErrorResponse("CODE", "message").getTraceId()).isEqualTo(TRACE_ID);
    }

    @Test
    void fallsBackToARandomIdWhenTracingIsOff() {
        assertThat(new ErrorResponse("CODE", "message").getTraceId()).hasSize(36);
    }
}
