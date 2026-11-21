package vn.edu.fpt.cinema.booking.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 409 that also names the seats the customer lost, so the client can point at them. */
public class SeatUnavailableException extends BookingException {
    private final List<String> seatIds;

    public SeatUnavailableException(List<String> seatIds) {
        super(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", "Ghế không còn trống.");
        this.seatIds = List.copyOf(seatIds);
    }

    public List<String> seatIds() { return seatIds; }
}
