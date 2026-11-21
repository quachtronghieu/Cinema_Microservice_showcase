package vn.edu.fpt.cinema.booking;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;

class BookingSchemaIT extends SqlServerIT {
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void showtime() {
        DbReset.clean(jdbc);
        jdbc.update("""
            INSERT INTO dbo.Sales_Snapshot (showtime_id, snapshot_version, sales_status, movie_id, movie_title, room_id, room_name, show_date, start_time, end_time)
            VALUES ('SW1', 1, 'OPEN', 'MV1', N'Movie', 1, N'Room 1', '2026-12-01', '19:00', '21:00')""");
    }

    private void seat(String id, String templateSeatId, String status, String bookingId) {
        jdbc.update("""
            INSERT INTO dbo.Showtime_Seat (showtime_seat_id, showtime_id, template_seat_id, row_label, seat_number, seat_type, unit_price, status, booking_id)
            VALUES (?, 'SW1', ?, 'A', ?, 'STANDARD', 80000, ?, ?)""", id, templateSeatId, id.hashCode() & 0xff, status, bookingId);
    }

    @Test void sameTemplateSeatTwiceInOneShowtime_rejected() {
        seat("SS1", "TS1", "AVAILABLE", null);
        assertThatThrownBy(() -> seat("SS2", "TS1", "AVAILABLE", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void soldSeatWithoutBookingOwner_rejected() {
        assertThatThrownBy(() -> seat("SS1", "TS1", "SOLD", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void idempotencyKey_caseSensitive_butUniquePerActorAndOperation() {
        String sql = """
            INSERT INTO dbo.IdempotencyRecord (actor_id, operation, idem_key, request_hash, response_status, created_at, expires_at)
            VALUES ('CS1', 'create-booking', ?, REPLICATE('a', 64), 201, SYSUTCDATETIME(), DATEADD(HOUR, 24, SYSUTCDATETIME()))""";
        jdbc.update(sql, "key-abc");
        jdbc.update(sql, "KEY-ABC");
        assertThatThrownBy(() -> jdbc.update(sql, "key-abc")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void voucherUsedPlusReservedAboveLimit_rejected() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status)
            VALUES ('VC1', 'FPT', N'FPT', 'PERCENTAGE', 10, 1, 1, 1, 1)""")).isInstanceOf(DataIntegrityViolationException.class);
    }
}
