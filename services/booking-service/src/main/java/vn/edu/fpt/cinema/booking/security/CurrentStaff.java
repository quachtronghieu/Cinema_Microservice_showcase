package vn.edu.fpt.cinema.booking.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.edu.fpt.cinema.booking.exception.BookingException;

/** The staffId claim identity puts in ADMIN tokens; recorded as who checked a ticket in. */
public final class CurrentStaff {
    private CurrentStaff() {}

    public static String id(Jwt jwt) {
        String staffId = jwt.getClaimAsString("staffId");
        if (staffId == null || staffId.isBlank()) {
            throw new BookingException(HttpStatus.FORBIDDEN, "STAFF_ACCOUNT_REQUIRED", "An admin staff account is required.");
        }
        return staffId;
    }
}
