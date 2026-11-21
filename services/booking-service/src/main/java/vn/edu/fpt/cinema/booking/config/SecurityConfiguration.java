package vn.edu.fpt.cinema.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import vn.edu.fpt.cinema.booking.exception.ApiError;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    /** Scope cinema-service needs to open and close sales here (AGENTS 25). */
    public static final String SALES_WRITE = "SCOPE_sales:write";

    @Bean
    JwtDecoder jwtDecoder(@Value("${booking.security.issuer}") String issuer,
                          @Value("${booking.security.jwk-set-uri}") String jwks,
                          @Value("${booking.security.audience}") String audience,
                          @Value("${booking.security.service-audience}") String serviceAudience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validators(issuer, audience, serviceAudience));
        return decoder;
    }

    /** User tokens are for the public API audience; service tokens must name this service as their audience. */
    public static OAuth2TokenValidator<Jwt> validators(String issuer, String audience, String serviceAudience) {
        OAuth2TokenValidator<Jwt> required = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getNotBefore() != null
                && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getAudience().contains(isServiceToken(jwt) ? serviceAudience : audience);
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Required token claims are missing or invalid", null));
        };
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ofSeconds(30)), new JwtIssuerValidator(issuer), required);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, ObjectProvider<Tracer> tracer) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // A service token never acts as a user: it gets its scopes only, never roles.
            if (isServiceToken(jwt)) {
                String scope = jwt.getClaimAsString("scope");
                return scope == null ? List.of() : Arrays.stream(scope.trim().split("\\s+")).filter(s -> !s.isEmpty())
                    .<GrantedAuthority>map(s -> new SimpleGrantedAuthority("SCOPE_" + s)).toList();
            }
            if (!(jwt.getClaims().get("roles") instanceof Collection<?> roles)) return List.of();
            return roles.stream().filter(String.class::isInstance).map(String.class::cast)
                .filter(role -> role.equals("USER") || role.equals("ADMIN"))
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        http.addFilterBefore(new TraceIdFilter(tracer), SecurityContextHolderFilter.class)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Prometheus scrapes without a token; the gateway has no route to /actuator, so it stays internal.
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/prometheus",
                    "/api/v1/showtimes/*/seat-availability").permitAll()
                .requestMatchers("/api/v1/internal/**").hasAuthority(SALES_WRITE)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/customer/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, e) -> write(mapper, req, res, 401, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((req, res, e) -> write(mapper, req, res, 403, "FORBIDDEN", "Insufficient permission")))
            .oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req, res, e) -> write(mapper, req, res, 401, "UNAUTHORIZED", "Authentication required")));
        return http.build();
    }

    private static boolean isServiceToken(Jwt jwt) {
        return "service".equals(jwt.getClaimAsString("token_kind"));
    }

    private static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                              int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ApiError.of(code, message, request));
    }
}
