package vn.edu.fpt.cinema.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.*;

@Configuration
public class ClockConfiguration {
    // Release dates are cinema calendar days, so "today" must not follow a container's UTC default.
    @Bean Clock catalogClock(@Value("${catalog.business-zone}") String zone) { return Clock.system(ZoneId.of(zone)); }
}
