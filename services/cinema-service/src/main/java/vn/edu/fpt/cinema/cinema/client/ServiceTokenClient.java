package vn.edu.fpt.cinema.cinema.client;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;

/** Gets this service's own token from identity (client credentials) and reuses it until shortly before expiry. */
@Component
public class ServiceTokenClient {
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    record Cached(String token, Instant refreshAt) {}

    record TokenResponse(String accessToken, long expiresIn) {}

    @Autowired
    public ServiceTokenClient(@Value("${services.identity.url:http://localhost:8081}") String identityUrl,
                              @Value("${cinema.service-client.id:svc:cinema-service}") String clientId,
                              @Value("${cinema.service-client.secret:}") String clientSecret,
                              RestClient.Builder builder) {
        this(builder.baseUrl(identityUrl).requestFactory(timeouts()).build(), clientId, clientSecret, Clock.systemUTC());
    }

    ServiceTokenClient(RestClient restClient, String clientId, String clientSecret, Clock clock) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    /** "Bearer ..." for a call to targetService with the given scope; 503 when identity cannot issue one. */
    public String bearer(String targetService, String scope) {
        String key = targetService + " " + scope;
        Cached cached = cache.get(key);
        if (cached == null || !clock.instant().isBefore(cached.refreshAt())) {
            cached = fetch(targetService, scope);
            cache.put(key, cached);
        }
        return "Bearer " + cached.token();
    }

    private Cached fetch(String targetService, String scope) {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw unavailable("cinema.service-client.secret is not configured");
        }
        try {
            TokenResponse response = restClient.post().uri("/api/v1/auth/token/service")
                .body(Map.of("clientId", clientId, "clientSecret", clientSecret, "targetService", targetService, "scope", scope))
                .retrieve().body(TokenResponse.class);
            if (response == null || response.accessToken() == null) throw unavailable("empty token response");
            // Renew 30 s early so a token never expires on its way to the other service.
            return new Cached(response.accessToken(), clock.instant().plusSeconds(Math.max(0, response.expiresIn() - 30)));
        } catch (CinemaException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable(e.getMessage());
        }
    }

    private static CinemaException unavailable(String reason) {
        return new CinemaException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_TOKEN_UNAVAILABLE",
            "Không lấy được token nội bộ từ identity-service: " + reason);
    }

    static SimpleClientHttpRequestFactory timeouts() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // AGENTS 8: 2 s connect, 3 s read
        factory.setReadTimeout(3000);
        return factory;
    }
}
