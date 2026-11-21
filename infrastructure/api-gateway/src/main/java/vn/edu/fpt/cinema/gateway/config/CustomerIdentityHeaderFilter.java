package vn.edu.fpt.cinema.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Booking identifies customers by X-User-Id, so a client must never be able to choose its value.
@Component
public class CustomerIdentityHeaderFilter implements GlobalFilter, Ordered {
    static final String HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal().ofType(JwtAuthenticationToken.class)
            .map(token -> {
                String customerId = token.getToken().getClaimAsString("customerId");
                return customerId == null ? "" : customerId;
            })
            .defaultIfEmpty("")
            .flatMap(customerId -> chain.filter(exchange.mutate().request(request -> request.headers(headers -> {
                headers.remove(HEADER);
                if (!customerId.isBlank()) headers.set(HEADER, customerId);
            })).build()));
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
