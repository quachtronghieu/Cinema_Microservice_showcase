package vn.edu.fpt.cinema.cinema.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;

/**
 * Open/close sales in booking-service (AGENTS 4). Every call either succeeds or throws, so cinema never shows a
 * state that booking did not confirm.
 */
@Component
public class BookingClient {
    static final String TARGET = "booking-service";
    static final String SCOPE = "sales:write";
    private static final Logger log = LoggerFactory.getLogger(BookingClient.class);

    private final RestClient restClient;
    private final ServiceTokenClient tokens;

    @Autowired
    public BookingClient(@Value("${services.booking.url:http://localhost:8083}") String bookingBaseUrl, ServiceTokenClient tokens,
                         RestClient.Builder builder) {
        this(builder.baseUrl(bookingBaseUrl).requestFactory(ServiceTokenClient.timeouts()).build(), tokens);
    }

    BookingClient(RestClient restClient, ServiceTokenClient tokens) {
        this.restClient = restClient;
        this.tokens = tokens;
    }

    public void initSales(InitSalesRequest request) {
        call(() -> restClient.post().uri("/api/v1/internal/showtimes/init-sales")
            .header("Authorization", tokens.bearer(TARGET, SCOPE)).body(request).retrieve().toBodilessEntity());
        log.info("Booking opened sales for showtime {}", request.showtimeId());
    }

    public void closeSales(String showtimeId) {
        call(() -> restClient.post().uri("/api/v1/internal/showtimes/{id}/close-sales", showtimeId)
            .header("Authorization", tokens.bearer(TARGET, SCOPE)).retrieve().toBodilessEntity());
        log.info("Booking closed sales for showtime {}", showtimeId);
    }

    private static void call(Runnable request) {
        try {
            request.run();
        } catch (CinemaException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                // Booking's own decision (for example SALES_CLOSED): pass it on unchanged.
                Map<?, ?> body = e.getResponseBodyAs(Map.class);
                throw new CinemaException(HttpStatus.CONFLICT, body != null && body.get("code") != null ? body.get("code").toString() : "BOOKING_CONFLICT",
                    body != null && body.get("message") != null ? body.get("message").toString() : "booking-service từ chối thao tác.");
            }
            throw unavailable(e.getStatusCode().value() + " " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw unavailable(e.getMessage());
        }
    }

    private static CinemaException unavailable(String reason) {
        log.warn("booking-service call failed: {}", reason);
        return new CinemaException(HttpStatus.SERVICE_UNAVAILABLE, "BOOKING_UNAVAILABLE",
            "booking-service chưa xác nhận thao tác, trạng thái mở/đóng bán chưa thay đổi. Vui lòng thử lại.");
    }

    public record InitSalesRequest(
        String showtimeId,
        String movieId,
        String movieTitle,
        Integer roomId,
        String roomName,
        LocalDate showDate,
        LocalTime startTime,
        LocalTime endTime,
        List<SeatItem> seats
    ) {
        public record SeatItem(
            String templateSeatId,
            String rowLabel,
            int seatNumber,
            String seatType,
            BigDecimal unitPrice
        ) {}
    }
}
