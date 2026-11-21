package vn.edu.fpt.cinema.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiError> booking(BookingException e, HttpServletRequest request) {
        return ResponseEntity.status(e.status()).body(ApiError.of(e.code(), e.getMessage(), request));
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<SeatConflictError> seatUnavailable(SeatUnavailableException e, HttpServletRequest request) {
        return ResponseEntity.status(e.status())
            .body(new SeatConflictError(e.code(), e.getMessage(), ApiError.trace(request), Map.of(), e.seatIds()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, List<String>> errors = new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>()).add(error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", validationMessage(request), ApiError.trace(request), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        Map<String, List<String>> errors = new TreeMap<>();
        e.getConstraintViolations().forEach(violation ->
            errors.computeIfAbsent(violation.getPropertyPath().toString(), key -> new ArrayList<>()).add(violation.getMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", validationMessage(request), ApiError.trace(request), errors));
    }

    private static String validationMessage(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/admin/vouchers") ? "Invalid voucher data" : "Invalid booking data";
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", "Invalid request input", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return ResponseEntity.status(405).body(ApiError.of("METHOD_NOT_ALLOWED", "HTTP method not supported for this path", request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> notFound(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(404).body(ApiError.of("NOT_FOUND", "Resource not found", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException e, HttpServletRequest request) {
        return ResponseEntity.status(403).body(ApiError.of("FORBIDDEN", "Insufficient permission", request));
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> unavailable(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(503).body(ApiError.of("SERVICE_UNAVAILABLE", "Booking temporarily unavailable", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        LoggerFactory.getLogger(getClass()).error("Booking failure traceId={}", ApiError.trace(request), e);
        return ResponseEntity.internalServerError().body(ApiError.of("INTERNAL_ERROR", "Unable to process request", request));
    }
}
