package vn.edu.fpt.cinema.cinema.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

/** Puts the distributed trace ID on logs, error bodies and the room audit so each can be looked up in Zipkin. */
public class TraceIdFilter extends OncePerRequestFilter {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private final ObjectProvider<Tracer> tracer;

    public TraceIdFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = traceId(request);
        request.setAttribute("traceId", id);
        response.setHeader("X-Trace-Id", id);
        try (var ignored = MDC.putCloseable("traceId", id)) { chain.doFilter(request, response); }
    }

    private String traceId(HttpServletRequest request) {
        var active = tracer.getIfAvailable();
        var span = active == null ? null : active.currentSpan();
        String id = span == null ? null : span.context().traceId();
        if (id != null && !id.isBlank()) return id; // The no-op tracer reports a blank ID.
        String incoming = request.getHeader("X-Trace-Id");
        return incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
