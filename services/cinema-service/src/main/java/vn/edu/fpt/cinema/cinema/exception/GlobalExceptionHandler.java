package vn.edu.fpt.cinema.cinema.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Every error uses the common body {code, message, traceId, fieldErrors} (AGENTS 6). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CinemaException.class)
    public ResponseEntity<ApiError> handleCinema(CinemaException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status()).body(ApiError.of(ex.code(), ex.getMessage(), request));
    }

    @ExceptionHandler(ShowtimeExpiredException.class)
    public ResponseEntity<ApiError> handleShowtimeExpired(ShowtimeExpiredException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("SHOWTIME_EXPIRED", ex.getMessage(), request));
    }

    @ExceptionHandler(ShowtimeNotFoundException.class)
    public ResponseEntity<ApiError> handleShowtimeNotFound(ShowtimeNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("SHOWTIME_NOT_FOUND", ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("CONFLICT", ex.getMessage(), request));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadInput(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", "Dữ liệu gửi lên không hợp lệ.", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiError.of("METHOD_NOT_ALLOWED", "HTTP method not supported for this path", request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("NOT_FOUND", "Resource not found", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of("FORBIDDEN", "Insufficient permission", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        LoggerFactory.getLogger(getClass()).error("Cinema failure traceId={}", ApiError.trace(request), ex);
        return ResponseEntity.internalServerError().body(ApiError.of("INTERNAL_ERROR", "Unable to process request", request));
    }
}
