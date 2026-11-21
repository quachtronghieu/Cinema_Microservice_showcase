package vn.edu.fpt.cinema.catalog.exception;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
public record ApiError(String code, String message, String traceId, Map<String,List<String>> fieldErrors) {
    public static ApiError of(String code, String message, HttpServletRequest request) {
        return new ApiError(code, message, trace(request), Map.of());
    }
    public static String trace(HttpServletRequest request) {
        var trace = request.getAttribute("traceId");
        return trace == null ? UUID.randomUUID().toString() : trace.toString();
    }
}
