package vn.edu.fpt.cinema.cinema.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Common error body (AGENTS 6): code, message, traceId, fieldErrors. */
public record ApiError(String code, String message, String traceId, Map<String, List<String>> fieldErrors) {
    public static ApiError of(String code, String message, HttpServletRequest request) {
        return new ApiError(code, message, trace(request), Map.of());
    }

    public static String trace(HttpServletRequest request) {
        Object trace = request.getAttribute("traceId");
        return trace == null ? UUID.randomUUID().toString() : trace.toString();
    }
}
