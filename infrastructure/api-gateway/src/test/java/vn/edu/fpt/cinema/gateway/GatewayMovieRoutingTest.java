package vn.edu.fpt.cinema.gateway;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "eureka.client.enabled=false", "spring.cloud.loadbalancer.cache.enabled=false",
    "management.zipkin.tracing.export.enabled=false"
})
@AutoConfigureObservability(metrics = false)
@LoadBalancerClient(name="catalog-service", configuration=GatewayMovieRoutingTest.Instances.class)
class GatewayMovieRoutingTest {
    static final String ADMIN = "/api/v1/admin/movies";
    static final String PUBLIC = "/api/v1/movies";
    static final RSAKey KEY = key();
    static final HttpServer UPSTREAM = upstream();
    static final AtomicInteger REQUESTS = new AtomicInteger();
    static final AtomicReference<String> TRACEPARENT = new AtomicReference<>();
    static final AtomicReference<String> CONTENT_TYPE = new AtomicReference<>();
    static final AtomicInteger BODY_SIZE = new AtomicInteger();
    static final AtomicReference<String> USER_ID = new AtomicReference<>();
    @Autowired WebTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("gateway.security.issuer", () -> "issuer.test");
        props.add("gateway.security.audience", () -> "cinema-api");
        props.add("gateway.security.jwk-set-uri", () -> "http://localhost:" + UPSTREAM.getAddress().getPort() + "/jwks");
    }
    public static class Instances {
        @Bean ServiceInstanceListSupplier catalogInstances() {
            return new ServiceInstanceListSupplier() {
                public String getServiceId() { return "catalog-service"; }
                public Flux<List<ServiceInstance>> get() {
                    return Flux.just(List.<ServiceInstance>of(new DefaultServiceInstance("catalog-test", "catalog-service",
                        "localhost", UPSTREAM.getAddress().getPort(), false)));
                }
            };
        }
    }
    @BeforeEach void reset() { REQUESTS.set(0); TRACEPARENT.set(null); CONTENT_TYPE.set(null); BODY_SIZE.set(0); USER_ID.set(null); }
    @AfterAll static void shutdown() { UPSTREAM.stop(0); }

    @ParameterizedTest
    @ValueSource(strings={PUBLIC, PUBLIC + "/MV000001", "/assets/img/movies/gun.jpg", "/assets/img/movies/uploads/id.png"})
    void guestReadsRouteToCatalogWithoutToken(String uri) {
        client.get().uri(uri).exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @ParameterizedTest
    @ValueSource(strings={"POST", "PUT", "DELETE"})
    void publicPathIsReadOnly(String method) {
        client.method(HttpMethod.valueOf(method)).uri(PUBLIC + "/MV000001").exchange().expectStatus().isUnauthorized();
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void adminRequiresTokenAndRoleBeforeRouting() throws Exception {
        client.get().uri(ADMIN).exchange().expectStatus().isUnauthorized();
        String user = token("USER");
        client.get().uri(ADMIN).headers(h -> h.setBearerAuth(user)).exchange().expectStatus().isForbidden();
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void multipartPosterUploadPassesThroughIntact() throws Exception {
        var builder = new MultipartBodyBuilder();
        builder.part("title", "Dune");
        builder.part("image", new byte[1024 * 1024], MediaType.IMAGE_PNG).filename("poster.png");
        String admin = token("ADMIN");
        client.post().uri(ADMIN).headers(h -> h.setBearerAuth(admin))
            .body(BodyInserters.fromMultipartData(builder.build())).exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(CONTENT_TYPE.get()).startsWith("multipart/form-data");
        assertThat(BODY_SIZE.get()).isGreaterThan(1024 * 1024);
    }
    @Test void clientCannotChooseTheCustomerIdentitySentDownstream() throws Exception {
        String customer = token("USER", "real-customer");
        client.get().uri("/api/v1/customer/watchlist").headers(h -> { h.setBearerAuth(customer); h.set("X-User-Id", "victim"); })
            .exchange().expectStatus().isOk();
        assertThat(USER_ID.get()).isEqualTo("real-customer");
    }
    @Test void spoofedIdentityIsRemovedWhenTheTokenHasNoCustomer() throws Exception {
        String admin = token("ADMIN");
        client.get().uri("/api/v1/customer/watchlist").headers(h -> { h.setBearerAuth(admin); h.set("X-User-Id", "victim"); })
            .exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(USER_ID.get()).isNull();
    }
    @Test void spoofedIdentityIsRemovedOnPublicRoutes() {
        client.get().uri(PUBLIC).header("X-User-Id", "victim").exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(USER_ID.get()).isNull();
    }
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/admin/showtimes", "/api/v1/admin/showtimes/ST000001", "/api/v1/admin/rooms", "/api/v1/admin/rooms/R1"})
    void cinemaAdminRoutesRequireAdminThenReachRouting(String uri) throws Exception {
        client.get().uri(uri).exchange().expectStatus().isUnauthorized();
        String user = token("USER");
        client.get().uri(uri).headers(h -> h.setBearerAuth(user)).exchange().expectStatus().isForbidden();
        assertThat(REQUESTS.get()).isZero();
        String admin = token("ADMIN");
        client.get().uri(uri).headers(h -> h.setBearerAuth(admin)).exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @Test void profileRouteRequiresASignedInUser() throws Exception {
        client.get().uri("/api/v1/customers/profile/me").exchange().expectStatus().isUnauthorized();
        assertThat(REQUESTS.get()).isZero();
        String user = token("USER");
        client.get().uri("/api/v1/customers/profile/me").headers(h -> h.setBearerAuth(user)).exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @Test void retiredStaffRoutesStayDenied() throws Exception {
        String admin = token("ADMIN");
        client.get().uri("/api/admin/staffs").headers(h -> h.setBearerAuth(admin)).exchange().expectStatus().isForbidden();
    }
    @Test void publicShowtimesNeedNoToken() {
        client.get().uri("/api/v1/showtimes?movieId=MV1&date=2026-11-04").exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/customer/watchlist", "/api/v1/customer/bookings", "/api/v1/customer/seat-holds", "/api/v1/customer/tickets"})
    void customerRoutesRequireASignedInUserThenReachRouting(String uri) throws Exception {
        client.get().uri(uri).exchange().expectStatus().isUnauthorized();
        assertThat(REQUESTS.get()).isZero();
        String user = token("USER");
        client.get().uri(uri).headers(h -> h.setBearerAuth(user)).exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/cinema/internal/seats/lock", "/api/cinema/internal/seats/lock", "/api/admin/rooms", "/api/admin/showtimes", "/api/customer/watchlist", "/api/customer/bookings"})
    void internalAndReplacedPathsAreNotReachable(String uri) throws Exception {
        String admin = token("ADMIN");
        client.post().uri(uri).headers(h -> h.setBearerAuth(admin)).exchange().expectStatus().isForbidden();
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void downstreamReceivesTheSameTraceIdReturnedToTheClient() {
        var result = client.get().uri(PUBLIC).exchange().expectStatus().isOk().returnResult(String.class);
        String traceId = result.getResponseHeaders().getFirst("X-Trace-Id");
        assertThat(traceId).matches("[0-9a-f]{32}");
        assertThat(TRACEPARENT.get()).startsWith("00-" + traceId + "-");
    }
    @Test void downstreamTraceHeaderIsNotDuplicatedForTheClient() {
        var result = client.get().uri(PUBLIC).exchange().expectStatus().isOk().returnResult(String.class);
        assertThat(result.getResponseHeaders().get("X-Trace-Id")).hasSize(1);
    }
    @Test void inboundTraceIsContinuedAcrossTheGateway() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        client.get().uri(PUBLIC).header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01").exchange()
            .expectStatus().isOk().expectHeader().valueEquals("X-Trace-Id", traceId);
        assertThat(TRACEPARENT.get()).startsWith("00-" + traceId + "-");
    }
    @Test void gatewayErrorBodyCarriesTheTraceId() {
        var result = client.get().uri(ADMIN).exchange().expectStatus().isUnauthorized().expectBody();
        String header = result.returnResult().getResponseHeaders().getFirst("X-Trace-Id");
        assertThat(header).matches("[0-9a-f]{32}");
        result.jsonPath("$.traceId").isEqualTo(header);
    }
    static RSAKey key() {
        try { return new RSAKeyGenerator(2048).keyID("test-key").generate(); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static String token(String role) throws Exception { return token(role, null); }
    static String token(String role, String customerId) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer("issuer.test").audience("cinema-api").subject("principal-test")
            .claim("roles", List.of(role)).claim("customerId", customerId).issueTime(Date.from(now)).notBeforeTime(Date.from(now.minusSeconds(1)))
            .expirationTime(Date.from(now.plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(KEY));
        return jwt.serialize();
    }
    static HttpServer upstream() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String payload = "{}";
                if (exchange.getRequestURI().getPath().equals("/jwks")) payload = "{\"keys\":[" + KEY.toPublicJWK().toJSONString() + "]}";
                else {
                    REQUESTS.incrementAndGet();
                    TRACEPARENT.set(exchange.getRequestHeaders().getFirst("traceparent"));
                    CONTENT_TYPE.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                    USER_ID.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
                    BODY_SIZE.set(exchange.getRequestBody().readAllBytes().length);
                }
                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("X-Trace-Id", "set-by-catalog");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
}
