package vn.edu.fpt.cinema.booking.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.edu.fpt.cinema.booking.exception.BookingException;

/** The customer id always comes from the verified token, never from the request body. */
public final class CurrentCustomer {
    private CurrentCustomer() {}

    public static String id(Jwt jwt) {
        String customerId = jwt.getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new BookingException(HttpStatus.FORBIDDEN, "CUSTOMER_ACCOUNT_REQUIRED", "A customer account is required.");
        }
        return customerId;
    }
}
