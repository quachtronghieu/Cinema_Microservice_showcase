package vn.edu.fpt.cinema.identity.exception;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.fpt.cinema.identity.dto.ErrorResponse;

/**
 * Global Exception Handler for Identity Service.
 * Formats all exceptions into standard AGENTS.md Section 6 response:
 * { code, message, traceId, fieldErrors }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = ErrorResponse.currentTraceId();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed for request payload",
                traceId,
                fieldErrors
        );
        log.warn("[Trace: {}] Validation error: {}", traceId, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        String traceId = ErrorResponse.currentTraceId();
        ErrorResponse response = new ErrorResponse(
                "AUTH_INVALID_CREDENTIALS",
                "Authentication failed: " + ex.getMessage(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        String traceId = ErrorResponse.currentTraceId();
        ErrorResponse response = new ErrorResponse(
                "AUTH_FORBIDDEN",
                "Access denied: You do not have permission to access this resource",
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        String traceId = ErrorResponse.currentTraceId();
        ErrorResponse response = new ErrorResponse(
                "HTTP_" + ex.getStatusCode().value(),
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                traceId,
                null
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = ErrorResponse.currentTraceId();
        ErrorResponse response = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = ErrorResponse.currentTraceId();
        log.error("[Trace: {}] Unexpected server error: {}", traceId, ex.getMessage(), ex);
        // Never leak SQL or stacktrace
        ErrorResponse response = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact support with traceId: " + traceId,
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
