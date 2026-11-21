package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static vn.edu.fpt.cinema.booking.support.QuoteFixtures.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest.ConcessionItem;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.*;

class PricingServiceTest {
    SeatHoldRepository holds = mock(SeatHoldRepository.class);
    ShowtimeSeatRepository seats = mock(ShowtimeSeatRepository.class);
    SalesSnapshotRepository showtimes = mock(SalesSnapshotRepository.class);
    VoucherRepository vouchers = mock(VoucherRepository.class);
    CatalogClient catalog = mock(CatalogClient.class);
    PricingService pricing = new PricingService(holds, seats, showtimes, vouchers, catalog, CLOCK);

    @BeforeEach void heldSeatsAndCoca() {
        when(holds.findById("H1")).thenReturn(Optional.of(activeHold()));
        when(showtimes.findById("SW1")).thenReturn(Optional.of(openShowtime()));
        when(seats.findByHoldId("H1")).thenReturn(heldSeats());
        when(catalog.concession(any(), eq("DR000002"))).thenReturn(coca());
    }

    @Test void twoStandardOneVipTwoCoca_totals350000() {
        var quote = pricing.quote(CUSTOMER, request(List.of(new ConcessionItem("DR000002", 2)), null));
        assertThat(quote.ticketSubtotal()).isEqualByComparingTo("260000");
        assertThat(quote.concessionSubtotal()).isEqualByComparingTo("90000");
        assertThat(quote.subtotal()).isEqualByComparingTo("350000.00");
        assertThat(quote.discountAmount()).isEqualByComparingTo("0");
        assertThat(quote.total()).isEqualByComparingTo("350000.00");
    }

    @Test void percentVoucher10_discount35000_total315000() {
        when(vouchers.findByCode("FPT")).thenReturn(Optional.of(voucher("PERCENTAGE", "10")));
        var quote = pricing.quote(CUSTOMER, request(List.of(new ConcessionItem("DR000002", 2)), " FPT "));
        assertThat(quote.discountAmount()).isEqualByComparingTo("35000.00");
        assertThat(quote.total()).isEqualByComparingTo("315000.00");
        assertThat(quote.voucher().getCode()).isEqualTo("FPT");
    }

    @Test void fixedVoucherLargerThanSubtotal_totalZero_notNegative() {
        when(vouchers.findByCode("FPT")).thenReturn(Optional.of(voucher("FIXED", "999999")));
        var quote = pricing.quote(CUSTOMER, request(List.of(), "FPT"));
        assertThat(quote.discountAmount()).isEqualByComparingTo("260000");
        assertThat(quote.total()).isEqualByComparingTo("0");
    }

    @Test void sameConcessionTwice_mergedIntoOneLine_quantitySummed() {
        var quote = pricing.quote(CUSTOMER, request(
            List.of(new ConcessionItem("DR000002", 1), new ConcessionItem("DR000002", 2)), null));
        assertThat(quote.concessionLines()).singleElement().satisfies(line -> {
            assertThat(line.quantity()).isEqualTo(3);
            assertThat(line.lineTotal()).isEqualByComparingTo("135000");
        });
        verify(catalog, times(1)).concession(any(), eq("DR000002"));
    }

    @Test void requestedSeatsDifferFromHeldSeats_409_HOLD_SELECTION_MISMATCH() {
        var other = new vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest("H1", "SW1", List.of("SS1", "SS9"), List.of(), null);
        assertThatThrownBy(() -> pricing.quote(CUSTOMER, other)).isInstanceOfSatisfying(BookingException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.code()).isEqualTo("HOLD_SELECTION_MISMATCH");
        });
    }

    @Test void exhaustedVoucher_409_VOUCHER_UNAVAILABLE() {
        when(vouchers.findByCode("FPT")).thenReturn(Optional.of(
            new vn.edu.fpt.cinema.booking.entity.Voucher("VC1", "FPT", "FIXED", new BigDecimal("10000"), 1, 0, 1, true)));
        assertThatThrownBy(() -> pricing.quote(CUSTOMER, request(List.of(), "FPT")))
            .isInstanceOfSatisfying(BookingException.class, e -> assertThat(e.code()).isEqualTo("VOUCHER_UNAVAILABLE"));
    }

    @Test void previewNeverWritesHoldSeatsOrVoucher() {
        when(vouchers.findByCode("FPT")).thenReturn(Optional.of(voucher("PERCENTAGE", "10")));
        pricing.quote(CUSTOMER, request(List.of(new ConcessionItem("DR000002", 1)), "FPT"));
        verify(holds, never()).save(any());
        verify(seats, never()).save(any());
        verify(seats, never()).saveAll(any());
        verify(vouchers, never()).save(any());
    }
}
