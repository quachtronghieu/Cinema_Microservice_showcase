package vn.edu.fpt.cinema.identity.dto;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Standard API Error Response complying strictly with AGENTS.md Section 6.
 * Form: { code, message, traceId, fieldErrors }
 */
public class ErrorResponse {

    private String code;
    private String message;
    private String traceId;
    private Map<String, String> fieldErrors;

    public ErrorResponse() {
        this.traceId = currentTraceId();
        this.fieldErrors = Collections.emptyMap();
    }

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.traceId = currentTraceId();
        this.fieldErrors = Collections.emptyMap();
    }

    public ErrorResponse(String code, String message, String traceId, Map<String, String> fieldErrors) {
        this.code = code;
        this.message = message;
        this.traceId = traceId != null ? traceId : currentTraceId();
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    /** The distributed trace ID, so an error can be looked up in Zipkin; a random ID when tracing is off. */
    public static String currentTraceId() {
        String id = MDC.get("traceId");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }
}
