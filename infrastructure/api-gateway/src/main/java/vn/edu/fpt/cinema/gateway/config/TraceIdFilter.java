package vn.edu.fpt.cinema.gateway.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(context -> {
            String traceId = traceId(context);
            exchange.getAttributes().put("traceId", traceId);
            exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);
            return chain.filter(exchange);
        });
    }

    // WebFlux starts the server observation before any WebFilter and only exposes it through the Reactor context.
    private static String traceId(ContextView context) {
        Observation observation = context.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
        var tracing = observation == null ? null
            : observation.getContextView().<TracingObservationHandler.TracingContext>get(TracingObservationHandler.TracingContext.class);
        var span = tracing == null ? null : tracing.getSpan();
        String id = span == null ? null : span.context().traceId();
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }
}
