package vn.edu.fpt.cinema.gateway.exception;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private final GatewayErrorResponses errors;
    public GatewayExceptionHandler(GatewayErrorResponses errors) { this.errors = errors; }
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) return Mono.error(error);
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectException || cause instanceof TimeoutException)
                return errors.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", "Service unavailable");
            if (cause.getCause() == cause) break;
        }
        if (error instanceof ResponseStatusException status) {
            if (status.getStatusCode().value() == 503 || status.getStatusCode().value() == 504)
                return errors.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", "Service unavailable");
            if (status.getStatusCode().value() == 404)
                return errors.write(exchange, HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
        }
        return errors.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }
}
