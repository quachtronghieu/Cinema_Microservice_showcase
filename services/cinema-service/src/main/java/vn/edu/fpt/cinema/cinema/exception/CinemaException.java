package vn.edu.fpt.cinema.cinema.exception;

import org.springframework.http.HttpStatus;

/** Business error with the HTTP status and stable code it maps to. */
public class CinemaException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CinemaException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }

    public String code() { return code; }
}
