package vn.edu.fpt.cinema.booking.security;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.edu.fpt.cinema.booking.exception.BookingException;

class CurrentCustomerTest {
    private static Jwt.Builder jwt() {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("p-1")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test void customerIdComesFromTokenClaim() {
        assertThat(CurrentCustomer.id(jwt().claim("customerId", "CS000003").build())).isEqualTo("CS000003");
    }

    @Test void tokenWithoutCustomerId_403_CUSTOMER_ACCOUNT_REQUIRED() {
        assertThatThrownBy(() -> CurrentCustomer.id(jwt().claim("staffId", "ST000001").build()))
            .isInstanceOfSatisfying(BookingException.class, e -> {
                assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(e.code()).isEqualTo("CUSTOMER_ACCOUNT_REQUIRED");
            });
    }
}
