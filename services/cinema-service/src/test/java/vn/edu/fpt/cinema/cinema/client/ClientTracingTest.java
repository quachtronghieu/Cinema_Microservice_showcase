package vn.edu.fpt.cinema.cinema.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.web.client.HttpClientObservationsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Every outgoing call must carry the current trace, otherwise Zipkin shows the flow cut into separate traces. */
class ClientTracingTest {

    @Test
    void outgoingCallsContinueTheCurrentTrace() throws Exception {
        Map<String, String> seen = new ConcurrentHashMap<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
            seen.put(exchange.getRequestURI().getPath(), traceparent == null ? "missing" : traceparent);
            byte[] body = "{\"accessToken\":\"t\",\"expiresIn\":300,\"title\":\"Movie\",\"duration\":90}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        String url = "http://localhost:" + server.getAddress().getPort();

        try {
            new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class, BraveAutoConfiguration.class,
                    MicrometerTracingAutoConfiguration.class, HttpClientObservationsAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class, RestClientAutoConfiguration.class))
                .withPropertyValues("services.booking.url=" + url, "services.identity.url=" + url,
                    "services.catalog.url=" + url, "cinema.service-client.secret=s")
                .withBean(ServiceTokenClient.class)
                .withBean(BookingClient.class)
                .withBean(CatalogMovieClient.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Tracer tracer = context.getBean(Tracer.class);
                    Span span = tracer.nextSpan().name("test").start();
                    try (var ignored = tracer.withSpan(span)) {
                        context.getBean(BookingClient.class).closeSales("S1");
                        context.getBean(CatalogMovieClient.class).getMovieById("M1");
                    } finally {
                        span.end();
                    }

                    String expected = "00-" + span.context().traceId() + "-";
                    assertThat(seen).containsOnlyKeys("/api/v1/auth/token/service",
                        "/api/v1/internal/showtimes/S1/close-sales", "/api/v1/movies/M1");
                    assertThat(seen.values()).allSatisfy(value -> assertThat(value).startsWith(expected));
                });
        } finally {
            server.stop(0);
        }
    }
}
