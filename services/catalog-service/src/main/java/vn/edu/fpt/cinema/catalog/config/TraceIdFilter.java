package vn.edu.fpt.cinema.catalog.config;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
public class TraceIdFilter extends OncePerRequestFilter {
    private final ObjectProvider<Tracer> tracer;
    public TraceIdFilter(ObjectProvider<Tracer> tracer) { this.tracer = tracer; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = traceId();
        request.setAttribute("traceId", id); response.setHeader("X-Trace-Id", id);
        try (var ignored = MDC.putCloseable("traceId", id)) { chain.doFilter(request, response); }
    }
    // Errors and the X-Trace-Id header carry the distributed trace ID so they can be looked up in Zipkin.
    private String traceId() {
        var active = tracer.getIfAvailable();
        var span = active == null ? null : active.currentSpan();
        String id = span == null ? null : span.context().traceId();
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id; // The no-op tracer reports a blank ID.
    }
}
