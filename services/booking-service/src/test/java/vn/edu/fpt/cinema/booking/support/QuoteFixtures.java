package vn.edu.fpt.cinema.booking.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import vn.edu.fpt.cinema.booking.client.CatalogClient;
import vn.edu.fpt.cinema.booking.entity.*;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest.ConcessionItem;

/** Shared data: hold H1 of customer CS1 on showtime SW1 holding two STANDARD seats and one VIP seat. */
public final class QuoteFixtures {
    public static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final String CUSTOMER = "CS1";

    private QuoteFixtures() {}

    public static SeatHold activeHold() { return new SeatHold("H1", CUSTOMER, "SW1", "ACTIVE", NOW.plusSeconds(120)); }
    public static SeatHold expiredHold() { return new SeatHold("H1", CUSTOMER, "SW1", "ACTIVE", NOW.minusSeconds(1)); }
    public static SalesSnapshot openShowtime() { return new SalesSnapshot("SW1", "OPEN"); }

    public static List<ShowtimeSeat> heldSeats() {
        return List.of(
            new ShowtimeSeat("SS1", "SW1", "A", 1, "STANDARD", new BigDecimal("80000.00"), "HELD", "H1"),
            new ShowtimeSeat("SS2", "SW1", "A", 2, "STANDARD", new BigDecimal("80000.00"), "HELD", "H1"),
            new ShowtimeSeat("SS3", "SW1", "B", 1, "VIP", new BigDecimal("100000.00"), "HELD", "H1"));
    }

    public static CatalogClient.Concession coca() {
        return new CatalogClient.Concession("DR000002", "Coca Cola", new BigDecimal("45000.00"), true);
    }

    public static Voucher voucher(String type, String value) {
        return new Voucher("VC1", "FPT", type, new BigDecimal(value), 10, 0, 0, true);
    }

    public static CustomerBookingRequest request(List<ConcessionItem> concessions, String voucherCode) {
        return new CustomerBookingRequest("H1", "SW1", List.of("SS1", "SS2", "SS3"), concessions, voucherCode);
    }
}
