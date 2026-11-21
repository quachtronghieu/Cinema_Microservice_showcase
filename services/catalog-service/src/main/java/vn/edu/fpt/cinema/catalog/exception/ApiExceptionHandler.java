package vn.edu.fpt.cinema.catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<ApiError> catalog(CatalogException e, HttpServletRequest request) {
        return ResponseEntity.status(e.status()).body(new ApiError(e.code(), e.getMessage(), ApiError.trace(request), e.fieldErrors()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String,List<String>> errors = new TreeMap<>();
        // A form binding failure's default message exposes Java conversion details.
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
                .add(error.isBindingFailure() ? "Invalid value" : error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Invalid request data", ApiError.trace(request), errors));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", "Invalid request input", request));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return ResponseEntity.status(413).body(ApiError.of("IMAGE_TOO_LARGE", "Image must not exceed 5 MB", request));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return ResponseEntity.status(415).body(ApiError.of("UNSUPPORTED_MEDIA_TYPE", "Unsupported content type", request));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return ResponseEntity.status(405).body(ApiError.of("METHOD_NOT_ALLOWED", "Method not allowed", request));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(404).body(ApiError.of("NOT_FOUND", "Resource not found", request));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException e, HttpServletRequest request) {
        return ResponseEntity.status(403).body(ApiError.of("FORBIDDEN", "Insufficient permission", request));
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> conflict(OptimisticLockingFailureException e, HttpServletRequest request) {
        if (request.getRequestURI().contains("/movies"))
            return ResponseEntity.status(409).body(ApiError.of("MOVIE_VERSION_CONFLICT", "Movie changed. Read it again before updating.", request));
        return ResponseEntity.status(409).body(ApiError.of("CONCESSION_VERSION_CONFLICT", "Concession changed. Read it again before updating.", request));
    }
    @ExceptionHandler({PessimisticLockingFailureException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> unavailable(DataAccessException e, HttpServletRequest request) {
        return ResponseEntity.status(503).body(ApiError.of("SERVICE_UNAVAILABLE", "Catalog temporarily unavailable", request));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        LoggerFactory.getLogger(getClass()).error("Catalog failure traceId={}", ApiError.trace(request), e);
        return ResponseEntity.internalServerError().body(ApiError.of("INTERNAL_ERROR", "Unable to process request", request));
    }
}
