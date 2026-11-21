package vn.edu.fpt.cinema.booking.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;

/** Prometheus (monitoring/prometheus.yml) scrapes booking without a token; nothing else in actuator opens up. */
@AutoConfigureMockMvc
@AutoConfigureObservability // Spring Boot turns metrics export off in tests unless asked.
class MetricsEndpointIT extends SqlServerIT {
    @Autowired MockMvc mvc;

    @Test void prometheusScrape_noToken_200_withApplicationLabel() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_memory_used_bytes")))
            .andExpect(content().string(containsString("application=\"booking-service\"")));
    }

    @Test void otherActuatorEndpointsStayClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator/metrics")).andExpect(status().is4xxClientError());
    }
}
