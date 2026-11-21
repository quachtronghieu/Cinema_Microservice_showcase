package vn.edu.fpt.cinema.cinema.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
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
import vn.edu.fpt.cinema.cinema.exception.ApiError;

/**
 * Cinema checks the token itself instead of trusting the gateway alone (AGENTS 7, 12).
 * Public showtime reads stay open; admin routes need ADMIN; everything else is denied.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    /** Showtime dates and times are the cinema's local values, so "now" is read in the same zone. */
    public static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    Clock cinemaClock() {
        return Clock.system(CINEMA_ZONE);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${cinema.security.issuer}") String issuer,
                          @Value("${cinema.security.jwk-set-uri}") String jwks,
                          @Value("${cinema.security.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validators(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> required = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getNotBefore() != null
                && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getAudience().contains(audience);
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
            if (!(jwt.getClaims().get("roles") instanceof Collection<?> roles)) return List.of();
            return roles.stream().filter(String.class::isInstance).map(String.class::cast)
                .filter(role -> role.equals("USER") || role.equals("ADMIN"))
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        http.addFilterBefore(new TraceIdFilter(tracer), SecurityContextHolderFilter.class)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                    "/api/v1/showtimes", "/api/v1/showtimes/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Internal seat lock/release has no service-token check yet (AGENTS 7), so it stays closed.
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, e) -> write(mapper, req, res, 401, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((req, res, e) -> write(mapper, req, res, 403, "FORBIDDEN", "Insufficient permission")))
            .oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req, res, e) -> write(mapper, req, res, 401, "UNAUTHORIZED", "Authentication required")));
        return http.build();
    }

    private static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                              int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ApiError.of(code, message, request));
    }
}
