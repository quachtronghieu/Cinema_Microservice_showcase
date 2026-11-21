package vn.edu.fpt.cinema.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayErrorResponses {
    private final ObjectMapper mapper;
    public GatewayErrorResponses(ObjectMapper mapper) { this.mapper = mapper; }
    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        var response = exchange.getResponse();
        if (response.isCommitted()) return Mono.empty();
        String traceId = exchange.getAttributeOrDefault("traceId", UUID.randomUUID().toString());
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Trace-Id", traceId);
        try {
            byte[] json = mapper.writeValueAsBytes(Map.of("code", code, "message", message,
                "traceId", traceId, "fieldErrors", Map.of()));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json)));
        } catch (JsonProcessingException e) { return Mono.error(e); }
    }
}
