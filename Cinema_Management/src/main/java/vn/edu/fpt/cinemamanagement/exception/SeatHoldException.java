package vn.edu.fpt.cinemamanagement.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Lỗi nghiệp vụ khi giữ/nhả ghế. {@code code} ổn định để client phân nhánh;
 * {@code seatIds} liệt kê ghế gây xung đột (nếu có).
 */
public class SeatHoldException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<String> seatIds;

    public SeatHoldException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public SeatHoldException(HttpStatus status, String code, String message, List<String> seatIds) {
        super(message);
        this.status = status;
        this.code = code;
        this.seatIds = seatIds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }
}
