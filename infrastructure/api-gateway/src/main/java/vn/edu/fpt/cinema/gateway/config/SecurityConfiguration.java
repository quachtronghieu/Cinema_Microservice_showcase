package vn.edu.fpt.cinema.gateway.config;

import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import vn.edu.fpt.cinema.gateway.exception.GatewayErrorResponses;

@Configuration
public class SecurityConfiguration {
    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${gateway.security.issuer}") String issuer,
                                  @Value("${gateway.security.jwk-set-uri}") String jwks,
                                  @Value("${gateway.security.audience}") String audience) {
        var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> required = jwt -> {
            boolean valid = jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getExpiresAt() != null && jwt.getNotBefore() != null
                && jwt.getAudience().contains(audience);
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Required claims are missing or invalid", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ofSeconds(30)), new JwtIssuerValidator(issuer), required));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http, GatewayErrorResponses errors,
            @Value("${gateway.cors.allowed-origins}") String origins) {
        var converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object claim = jwt.getClaims().get("roles");
            if (!(claim instanceof Collection<?> roles)) return Flux.empty();
            return Flux.fromIterable(roles).filter(role -> role instanceof String)
                .map(Object::toString).filter(role -> role.equals("USER") || role.equals("ADMIN"))
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role));
        });
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Requested-With"));
        cors.setExposedHeaders(List.of("Location", "X-Trace-Id"));
        var corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", cors);
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .cors(config -> config.configurationSource(corsSource))
            .authorizeExchange(auth -> auth
                .pathMatchers("/api/cashier/**").denyAll()
                .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                    "/.well-known/jwks.json", "/api/v1/auth/jwks.json", "/assets/img/concessions/**",
                    "/assets/img/movies/**", "/api/v1/movies", "/api/v1/movies/**",
                    "/api/v1/showtimes", "/api/v1/showtimes/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register",
                    "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                    "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .pathMatchers("/api/v1/customers/profile/**").hasAnyRole("USER", "ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/v1/concessions", "/api/v1/concessions/**").hasAnyRole("USER", "ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/movies/**", "/api/public/**", "/api/showtimes/**").permitAll()
                .pathMatchers("/api/v1/customer/**", "/api/v1/auth/me", "/api/v1/auth/logout",
                    "/api/auth/me", "/api/auth/logout", "/api/concessions/**").hasAnyRole("USER", "ADMIN")
                .pathMatchers("/api/rooms/**", "/actuator/**").hasRole("ADMIN")
                .anyExchange().denyAll())
            .exceptionHandling(config -> config
                .authenticationEntryPoint((exchange, e) -> errors.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((exchange, e) -> errors.write(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permission")))
            .oauth2ResourceServer(config -> config
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((exchange, e) -> errors.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((exchange, e) -> errors.write(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permission")));
        return http.build();
    }
}
