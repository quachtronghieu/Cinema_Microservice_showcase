package vn.edu.fpt.cinema.booking.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;

/** Writes booking_db rows directly, the same shape the seat-hold API and the sales sync will produce. */
public final class BookingTestData {
    private BookingTestData() {}

    /** Open showtime SW1 with seats SS1..SS4 (SS2 is VIP 100000, the others STANDARD 80000). */
    public static void showtimeWithFourSeats(JdbcTemplate jdbc) {
        jdbc.update("""
            INSERT INTO dbo.Sales_Snapshot (showtime_id, snapshot_version, sales_status, movie_id, movie_title, room_id, room_name, show_date, start_time, end_time)
            VALUES ('SW1', 1, 'OPEN', 'MV1', N'Dune', 1, N'Room 1', '2026-12-01', '19:00', '21:00')""");
        for (int i = 1; i <= 4; i++) {
            jdbc.update("""
                INSERT INTO dbo.Showtime_Seat (showtime_seat_id, showtime_id, template_seat_id, row_label, seat_number, seat_type, unit_price, status)
                VALUES (?, 'SW1', ?, 'A', ?, ?, ?, 'AVAILABLE')""",
                "SS" + i, "TS" + i, i, i == 2 ? "VIP" : "STANDARD", i == 2 ? 100000 : 80000);
        }
    }

    /** SeatHold + items + ActiveHold, seats HELD by the hold. */
    public static void hold(JdbcTemplate jdbc, String holdId, String customerId, Instant expiresAt, String... seatIds) {
        jdbc.update("INSERT INTO dbo.SeatHold (hold_id, customer_id, showtime_id, status, created_at, expires_at) VALUES (?, ?, 'SW1', 'ACTIVE', ?, ?)",
            holdId, customerId, utc(Instant.now()), utc(expiresAt));
        for (String seat : seatIds) {
            jdbc.update("UPDATE dbo.Showtime_Seat SET status = 'HELD', hold_id = ? WHERE showtime_seat_id = ?", holdId, seat);
            jdbc.update("INSERT INTO dbo.SeatHold_Item (hold_id, showtime_seat_id) VALUES (?, ?)", holdId, seat);
        }
        jdbc.update("INSERT INTO dbo.ActiveHold (customer_id, showtime_id, hold_id) VALUES (?, 'SW1', ?)", customerId, holdId);
    }

    /** DATETIME2 columns hold UTC (hibernate.jdbc.time_zone=UTC); Timestamp.from would shift by the JVM zone. */
    public static Timestamp utc(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    public static int count(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo." + table, Integer.class);
    }
}
