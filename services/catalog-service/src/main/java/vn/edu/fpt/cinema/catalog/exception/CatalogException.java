package vn.edu.fpt.cinema.catalog.exception;
import org.springframework.http.HttpStatus;
import java.util.*;
public class CatalogException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String,List<String>> fieldErrors;
    public CatalogException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }
    public CatalogException(HttpStatus status, String code, String message, Map<String,List<String>> fieldErrors) {
        super(message); this.status=status; this.code=code; this.fieldErrors=fieldErrors;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Map<String,List<String>> fieldErrors() { return fieldErrors; }
}
