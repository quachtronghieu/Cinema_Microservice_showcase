package vn.edu.fpt.cinema.cinema.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import vn.edu.fpt.cinema.cinema.entities.Showtime;

public record ShowtimeResponseDTO(
    String showtimeId,
    String movieId,
    String movieTitle,
    String roomId,
    LocalDate showDate,
    LocalTime startTime,
    LocalTime endTime,
    String status,
    String salesStatus
) {
    public static ShowtimeResponseDTO fromEntity(Showtime showtime, String movieTitle) {
        return new ShowtimeResponseDTO(
            showtime.getShowtimeId(),
            showtime.getMovieId(),
            movieTitle,
            showtime.getRoom() == null ? null : String.valueOf(showtime.getRoom().getId()),
            showtime.getShowDate(),
            showtime.getStartTime(),
            showtime.getEndTime(),
            showtime.getDisplayStatus(),
            showtime.getSalesStatus()
        );
    }
}
