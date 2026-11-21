package vn.edu.fpt.cinema.cinema.exception;

/**
 * Exception ném ra khi người dùng cố gắng đặt vé hoặc chọn suất chiếu đã qua thời gian bắt đầu.
 */
public class ShowtimeExpiredException extends RuntimeException {

    public ShowtimeExpiredException() {
        super("Không thể đặt vé cho suất chiếu đã qua.");
    }

    public ShowtimeExpiredException(String message) {
        super(message);
    }
}
