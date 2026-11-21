package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest.ConcessionItem;

class RequestHasherTest {
    private static CustomerBookingRequest request(List<String> seats, List<ConcessionItem> concessions, String voucher) {
        return new CustomerBookingRequest("H1", "SW1", seats, concessions, voucher);
    }

    @Test void seatOrderAndDuplicateConcessionLines_sameHash() {
        String a = RequestHasher.hash("create-booking", request(List.of("SS2", "SS1"),
            List.of(new ConcessionItem("DR1", 1), new ConcessionItem("PC1", 1), new ConcessionItem("DR1", 1)), " fpt "));
        String b = RequestHasher.hash("create-booking", request(List.of("SS1", "SS2"),
            List.of(new ConcessionItem("PC1", 1), new ConcessionItem("DR1", 2)), "FPT"));
        assertThat(a).isEqualTo(b);
    }

    @Test void differentQuantity_differentHash() {
        assertThat(RequestHasher.hash("create-booking", request(List.of("SS1"), List.of(new ConcessionItem("DR1", 1)), null)))
            .isNotEqualTo(RequestHasher.hash("create-booking", request(List.of("SS1"), List.of(new ConcessionItem("DR1", 2)), null)));
    }

    @Test void noConcessionsAndBlankVoucher_sameAsEmpty() {
        assertThat(RequestHasher.hash("create-booking", request(List.of("SS1"), null, "  ")))
            .isEqualTo(RequestHasher.hash("create-booking", request(List.of("SS1"), List.of(), null)));
    }

    @Test void operationIsPartOfTheHash_and64HexChars() {
        String booking = RequestHasher.hash("create-booking", request(List.of("SS1"), List.of(), null));
        assertThat(booking).matches("[0-9a-f]{64}")
            .isNotEqualTo(RequestHasher.hash("other-operation", request(List.of("SS1"), List.of(), null)));
    }
}
