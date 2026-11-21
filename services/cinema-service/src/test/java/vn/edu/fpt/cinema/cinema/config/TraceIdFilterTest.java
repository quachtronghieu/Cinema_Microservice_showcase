package vn.edu.fpt.cinema.cinema.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Test
    void activeSpanWinsOverTheHeader() throws Exception {
        TraceContext context = mock(TraceContext.class);
        when(context.traceId()).thenReturn(TRACE_ID);
        Span span = mock(Span.class);
        when(span.context()).thenReturn(context);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);

        assertThat(run(tracer, "from-header")).isEqualTo(TRACE_ID);
    }

    @Test
    void headerIsUsedWhenTracingIsOff() throws Exception {
        assertThat(run(null, "from-header")).isEqualTo("from-header");
    }

    @Test
    void unsafeHeaderIsReplaced() throws Exception {
        assertThat(run(null, "bad id\r\nX-Evil: 1")).hasSize(36).doesNotContain("Evil");
    }

    private static String run(Tracer tracer, String header) throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(tracer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", header);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceIdFilter(provider).doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute("traceId")).isEqualTo(response.getHeader("X-Trace-Id"));
        return response.getHeader("X-Trace-Id");
    }
}
