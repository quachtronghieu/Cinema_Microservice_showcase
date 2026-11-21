package vn.edu.fpt.cinema.gateway;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "eureka.client.enabled=false", "spring.cloud.loadbalancer.cache.enabled=false"
})
@LoadBalancerClients({
    @LoadBalancerClient(name="catalog-service", configuration=GatewaySecurityRoutingTest.Instances.class),
    @LoadBalancerClient(name="booking-service", configuration=GatewaySecurityRoutingTest.BookingInstances.class)})
class GatewaySecurityRoutingTest {
    static final String ADMIN = "/api/v1/admin/concessions";
    static final String CUSTOMER = "/api/v1/concessions";
    static final RSAKey KEY = key();
    static final HttpServer UPSTREAM = upstream();
    static final AtomicInteger REQUESTS = new AtomicInteger();
    static final AtomicReference<String> BODY = new AtomicReference<>();
    static final AtomicBoolean HAS_BEARER = new AtomicBoolean();
    static final AtomicBoolean AVAILABLE = new AtomicBoolean(true);
    static final AtomicInteger CATALOG_PORT = new AtomicInteger(UPSTREAM.getAddress().getPort());
    static final AtomicBoolean SLOW = new AtomicBoolean();
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
                    return Flux.defer(() -> Flux.just(AVAILABLE.get()
                        ? List.<ServiceInstance>of(new DefaultServiceInstance("catalog-test", "catalog-service", "localhost", CATALOG_PORT.get(), false))
                        : List.of()));
                }
            };
        }
    }
    public static class BookingInstances {
        @Bean ServiceInstanceListSupplier bookingInstances() {
            return new ServiceInstanceListSupplier() {
                public String getServiceId() { return "booking-service"; }
                public Flux<List<ServiceInstance>> get() {
                    return Flux.just(List.<ServiceInstance>of(new DefaultServiceInstance(
                        "booking-test", "booking-service", "localhost", UPSTREAM.getAddress().getPort(), false)));
                }
            };
        }
    }
    @BeforeEach void reset() {
        REQUESTS.set(0); BODY.set(null); HAS_BEARER.set(false); AVAILABLE.set(true);
        CATALOG_PORT.set(UPSTREAM.getAddress().getPort()); SLOW.set(false);
    }
    @AfterAll static void shutdown() { UPSTREAM.stop(0); }

    @ParameterizedTest
    @ValueSource(strings={"GET", "POST", "PUT", "DELETE"})
    void missingTokenRejectedBeforeRouting(String method) {
        error(client.method(HttpMethod.valueOf(method)).uri(ADMIN).exchange(), 401, "UNAUTHORIZED");
        assertThat(REQUESTS.get()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings={"GET", "POST", "PUT", "DELETE"})
    void userCannotCallAdminBeforeRouting(String method) throws Exception {
        error(client.method(HttpMethod.valueOf(method)).uri(ADMIN)
            .headers(h -> h.setBearerAuth(token("USER", "valid"))).exchange(), 403, "FORBIDDEN");
        assertThat(REQUESTS.get()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings={"issuer", "audience", "expired", "future", "signature", "missing-exp", "missing-nbf", "missing-sub", "algorithm"})
    void invalidJwtRejectedBeforeRouting(String variant) throws Exception {
        error(client.get().uri(ADMIN).headers(h -> h.setBearerAuth(token("ADMIN", variant))).exchange(), 401, "UNAUTHORIZED");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void legacyRoleIsNotAnAdmin() throws Exception {
        error(client.get().uri(ADMIN).headers(h -> h.setBearerAuth(token("CASHIER_STAFF", "valid"))).exchange(), 403, "FORBIDDEN");
        assertThat(REQUESTS.get()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings={"USER", "ADMIN"})
    void customerReadUsesLoadBalancerAndForwardsBearer(String role) throws Exception {
        client.get().uri(CUSTOMER + "?type=PC&page=0&size=20")
            .headers(h -> h.setBearerAuth(token(role, "valid"))).exchange()
            .expectStatus().isOk().expectBody().jsonPath("$.path").isEqualTo(CUSTOMER)
            .jsonPath("$.query").isEqualTo("type=PC&page=0&size=20");
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(HAS_BEARER.get()).isTrue();
    }
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/customer/bookings", "/api/v1/customer/bookings/B1", "/api/v1/customer/tickets", "/api/v1/customer/seat-holds"})
    void customerBookingPathsRouteToBookingService(String path) throws Exception {
        client.get().uri(path).headers(h -> h.setBearerAuth(token("USER", "valid"))).exchange()
            .expectStatus().isOk().expectBody().jsonPath("$.path").isEqualTo(path);
        assertThat(REQUESTS.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/admin/vouchers", "GET,/api/v1/admin/vouchers/VC1", "POST,/api/v1/admin/vouchers",
        "PATCH,/api/v1/admin/vouchers/VC1", "POST,/api/v1/admin/vouchers/VC1/disable"})
    void voucherOperationsRejectMissingOrUserTokenBeforeRouting(String method, String path) throws Exception {
        error(client.method(HttpMethod.valueOf(method)).uri(path).exchange(), 401, "UNAUTHORIZED");
        error(client.method(HttpMethod.valueOf(method)).uri(path)
            .headers(h -> h.setBearerAuth(token("USER", "valid"))).exchange(), 403, "FORBIDDEN");
        assertThat(REQUESTS.get()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/admin/vouchers,200", "GET,/api/v1/admin/vouchers/VC1,200",
        "POST,/api/v1/admin/vouchers,201", "PATCH,/api/v1/admin/vouchers/VC1,200",
        "POST,/api/v1/admin/vouchers/VC1/disable,204"})
    void adminVoucherOperationsRouteToBookingAndPreserveBodyAndStatus(String method, String path, int status) {
        String body = method.equals("PATCH") ? "{\"usageLimit\":120}" : "";
        var response = client.method(HttpMethod.valueOf(method)).uri(path)
            .headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body).exchange().expectStatus().isEqualTo(status);
        if (status == 201) response.expectHeader().valueEquals("Location", "/api/v1/admin/vouchers/test-id");
        if (status == 204) response.expectBody().isEmpty();
        else response.expectBody().jsonPath("$.path").isEqualTo(path);
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(HAS_BEARER.get()).isTrue();
        assertThat(BODY.get()).isEqualTo(body);
    }

    @Test void expiredVoucherAdminTokenDoesNotReachBooking() {
        error(client.get().uri("/api/v1/admin/vouchers")
            .headers(h -> h.setBearerAuth(token("ADMIN", "expired"))).exchange(), 401, "UNAUTHORIZED");
        assertThat(REQUESTS.get()).isZero();
    }

    @Test void voucherPatchPreflightIsAllowedWithoutCallingBooking() {
        client.options().uri("/api/v1/admin/vouchers/VC1").header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "PATCH")
            .header("Access-Control-Request-Headers", "authorization,content-type").exchange()
            .expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test
    void guestSeatAvailabilityRoutesToBookingNotCinema() {
        client.get().uri("/api/v1/showtimes/SW1/seat-availability").exchange()
            .expectStatus().isOk().expectBody().jsonPath("$.path").isEqualTo("/api/v1/showtimes/SW1/seat-availability");
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @Test
    void seatAvailabilityIsReadOnlyAtGateway() {
        error(client.post().uri("/api/v1/showtimes/SW1/seat-availability").exchange(), 401, "UNAUTHORIZED");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test
    void guestCannotHoldSeatsAtGateway() {
        error(client.post().uri("/api/v1/customer/seat-holds").exchange(), 401, "UNAUTHORIZED");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test
    void adminTicketCheckInRoutesToBookingService() throws Exception {
        client.post().uri("/api/v1/admin/tickets/T1/check-in")
            .headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).exchange()
            .expectStatus().isCreated().expectBody().jsonPath("$.path").isEqualTo("/api/v1/admin/tickets/T1/check-in");
    }
    @ParameterizedTest
    @ValueSource(strings={"GET", "POST", "PUT", "DELETE"})
    void adminCommandsUseLoadBalancerAndPreservePayload(String method) throws Exception {
        String body = "{\"name\":\"Popcorn\",\"version\":0}";
        String uri = method.equals("GET") || method.equals("POST") ? ADMIN : ADMIN + "/test-id?version=0";
        var response = client.method(HttpMethod.valueOf(method)).uri(uri)
            .headers(h -> h.setBearerAuth(token("ADMIN", "valid")))
            .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        if (method.equals("POST")) response.expectStatus().isCreated().expectHeader().valueEquals("Location", ADMIN + "/test-id");
        else if (method.equals("DELETE")) response.expectStatus().isNoContent().expectBody().isEmpty();
        else response.expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(HAS_BEARER.get()).isTrue();
        assertThat(BODY.get()).isEqualTo(body);
    }
    @Test void noInstanceReturnsCommon503() throws Exception {
        AVAILABLE.set(false);
        error(client.get().uri(ADMIN).headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).exchange(), 503, "DEPENDENCY_UNAVAILABLE");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void unavailableRegisteredInstanceReturnsCommon503() throws Exception {
        try (var unused = new ServerSocket(0)) { CATALOG_PORT.set(unused.getLocalPort()); }
        error(client.get().uri(ADMIN).headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).exchange(), 503, "DEPENDENCY_UNAVAILABLE");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void slowUpstreamReturnsCommon503WithinReadTimeout() throws Exception {
        SLOW.set(true);
        error(client.get().uri(ADMIN).headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).exchange(), 503, "DEPENDENCY_UNAVAILABLE");
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @Test void imagesArePublicAndUseCatalogRoute() {
        client.get().uri("/assets/img/concessions/popcorn.png").exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isEqualTo(1);
    }
    @Test void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void sensitiveActuatorRequiresAdmin() throws Exception {
        error(client.get().uri("/actuator/metrics").exchange(), 401, "UNAUTHORIZED");
        error(client.get().uri("/actuator/metrics").headers(h -> h.setBearerAuth(token("USER", "valid"))).exchange(), 403, "FORBIDDEN");
    }
    @Test void retiredCashierRoutesAreDenied() throws Exception {
        error(client.get().uri("/api/cashier/bookings").headers(h -> h.setBearerAuth(token("ADMIN", "valid"))).exchange(), 403, "FORBIDDEN");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void corsAcceptsConfiguredOrigin() {
        client.options().uri(CUSTOMER).header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization")
            .exchange().expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
        assertThat(REQUESTS.get()).isZero();
    }
    @Test void corsRejectsUnconfiguredOrigin() {
        client.options().uri(CUSTOMER).header("Origin", "https://untrusted.invalid")
            .header("Access-Control-Request-Method", "GET").exchange().expectStatus().isForbidden();
        assertThat(REQUESTS.get()).isZero();
    }
    static void error(WebTestClient.ResponseSpec response, int status, String code) {
        response.expectStatus().isEqualTo(status).expectHeader().exists("X-Trace-Id").expectBody()
            .jsonPath("$.code").isEqualTo(code).jsonPath("$.message").isNotEmpty()
            .jsonPath("$.traceId").isNotEmpty().jsonPath("$.fieldErrors").isMap();
    }
    static RSAKey key() {
        try { return new RSAKeyGenerator(2048).keyID("test-key").generate(); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static String token(String role, String variant) {
        try {
            Instant now = Instant.now();
            var claims = new JWTClaimsSet.Builder().issuer(variant.equals("issuer") ? "wrong" : "issuer.test")
                .audience(variant.equals("audience") ? "wrong" : "cinema-api").claim("roles", List.of(role))
                .issueTime(Date.from(now));
            if (!variant.equals("missing-sub")) claims.subject("principal-test");
            if (!variant.equals("missing-nbf")) claims.notBeforeTime(Date.from(variant.equals("future") ? now.plusSeconds(300) : now.minusSeconds(1)));
            if (!variant.equals("missing-exp")) claims.expirationTime(Date.from(variant.equals("expired") ? now.minusSeconds(300) : now.plusSeconds(300)));
            var jwt = new SignedJWT(new JWSHeader.Builder(variant.equals("algorithm") ? JWSAlgorithm.RS512 : JWSAlgorithm.RS256).keyID("test-key").build(), claims.build());
            jwt.sign(new RSASSASigner(variant.equals("signature") ? key() : KEY));
            return jwt.serialize();
        } catch (Exception e) { throw new IllegalStateException("Cannot create test token", e); }
    }
    static HttpServer upstream() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                boolean jwks = exchange.getRequestURI().getPath().equals("/jwks");
                String payload;
                int status = 200;
                if (jwks) payload = "{\"keys\":[" + KEY.toPublicJWK().toJSONString() + "]}";
                else {
                    REQUESTS.incrementAndGet();
                    BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    String auth = exchange.getRequestHeaders().getFirst("Authorization");
                    HAS_BEARER.set(auth != null && auth.startsWith("Bearer "));
                    if (SLOW.get()) {
                        try { Thread.sleep(4000); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    payload = "{\"path\":\"" + exchange.getRequestURI().getPath() + "\",\"query\":\"" + exchange.getRequestURI().getRawQuery() + "\"}";
                    if (exchange.getRequestMethod().equals("POST")) {
                        status = 201;
                        String resource = exchange.getRequestURI().getPath().equals("/api/v1/admin/vouchers")
                            ? "/api/v1/admin/vouchers" : ADMIN;
                        exchange.getResponseHeaders().set("Location", resource + "/test-id");
                    }
                    if (exchange.getRequestURI().getPath().startsWith("/api/v1/admin/vouchers/")
                        && exchange.getRequestURI().getPath().endsWith("/disable")
                        && exchange.getRequestMethod().equals("POST")) status = 204;
                    if (exchange.getRequestMethod().equals("DELETE")) status = 204;
                }
                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
                if (status != 204) exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
}
