package vn.edu.fpt.cinema.booking.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Empties booking_db in foreign-key order so every DB test starts from a clean schema. */
public final class DbReset {
    private DbReset() {}

    public static void clean(JdbcTemplate jdbc) {
        for (String table : new String[] {"Voucher_Reservation", "Payment", "Ticket", "Booking_Detail", "SeatHold_Item",
            "ActiveHold", "Showtime_Seat", "SeatHold", "Booking", "Voucher", "IdempotencyRecord", "Sales_Snapshot"}) {
            jdbc.update("DELETE FROM dbo." + table);
        }
    }
}
