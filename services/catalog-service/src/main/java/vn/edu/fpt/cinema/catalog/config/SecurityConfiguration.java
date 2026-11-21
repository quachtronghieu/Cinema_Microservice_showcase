package vn.edu.fpt.cinema.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import vn.edu.fpt.cinema.catalog.exception.ApiError;
import java.time.Duration;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    JwtDecoder jwtDecoder(@Value("${catalog.security.issuer}") String issuer,
                          @Value("${catalog.security.jwk-set-uri}") String jwks,
                          @Value("${catalog.security.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validators(issuer, audience));
        return decoder;
    }
    public static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
        var timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
        OAuth2TokenValidator<Jwt> required = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getNotBefore() != null
                && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getAudience().contains(audience);
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Required token claims are missing or invalid", null));
        };
        return new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(issuer), required);
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, ObjectProvider<Tracer> tracer) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object roles = jwt.getClaims().get("roles");
            if (!(roles instanceof Collection<?> list)) return List.of();
            return list.stream().filter(value -> value instanceof String)
                .map(Object::toString).filter(role -> role.equals("USER") || role.equals("ADMIN"))
                .<org.springframework.security.core.GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        http.addFilterBefore(new TraceIdFilter(tracer), SecurityContextHolderFilter.class)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/assets/img/concessions/**",
                    "/assets/img/movies/**", "/api/v1/movies", "/api/v1/movies/**").permitAll()
                .requestMatchers("/api/v1/customer/watchlist", "/api/v1/customer/watchlist/**").hasAnyRole("USER","ADMIN")
                .requestMatchers("/api/v1/admin/concessions", "/api/v1/admin/concessions/**",
                    "/api/v1/admin/movies", "/api/v1/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/concessions", "/api/v1/concessions/**").hasAnyRole("USER","ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,e) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    mapper.writeValue(response.getOutputStream(), ApiError.of("UNAUTHORIZED","Authentication required",request));
                })
                .accessDeniedHandler((request,response,e) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    mapper.writeValue(response.getOutputStream(), ApiError.of("FORBIDDEN","Insufficient permission",request));
                }))
            .oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((request,response,e) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    mapper.writeValue(response.getOutputStream(), ApiError.of("UNAUTHORIZED","Authentication required",request));
                }));
        return http.build();
    }
}
