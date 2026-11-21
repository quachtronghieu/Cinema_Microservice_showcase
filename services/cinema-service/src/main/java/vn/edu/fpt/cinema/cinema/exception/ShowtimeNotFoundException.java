package vn.edu.fpt.cinema.cinema.exception;

public class ShowtimeNotFoundException extends RuntimeException {
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
}
