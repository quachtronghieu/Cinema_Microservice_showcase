package vn.edu.fpt.cinema.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import vn.edu.fpt.cinema.cinema.entities.Showtime;

class EffectiveEndTest {
    static Showtime showtime(String date, String start, String end) {
        var showtime = new Showtime();
        showtime.setShowDate(LocalDate.parse(date));
        showtime.setStartTime(LocalTime.parse(start));
        showtime.setEndTime(LocalTime.parse(end));
        return showtime;
    }

    @Test void sameDayShowtimeEndsThatDay() {
        assertThat(RoomService.effectiveEnd(showtime("2026-11-06", "10:00", "12:15")))
            .isEqualTo(LocalDateTime.parse("2026-11-06T12:15"));
    }

    @Test void showtimePastMidnightEndsNextDay() {
        assertThat(RoomService.effectiveEnd(showtime("2026-11-06", "23:00", "01:15")))
            .isEqualTo(LocalDateTime.parse("2026-11-07T01:15"));
    }

    @Test void showtimeServiceEffectiveEndMatches() {
        assertThat(ShowtimeService.getEffectiveEnd(showtime("2026-11-06", "23:00", "01:15")))
            .isEqualTo(LocalDateTime.parse("2026-11-07T01:15"));
    }

    @Test void showtimeServiceEffectiveStartIsOnShowDate() {
        assertThat(ShowtimeService.getEffectiveStart(showtime("2026-11-06", "23:00", "01:15")))
            .isEqualTo(LocalDateTime.parse("2026-11-06T23:00"));
    }
}
