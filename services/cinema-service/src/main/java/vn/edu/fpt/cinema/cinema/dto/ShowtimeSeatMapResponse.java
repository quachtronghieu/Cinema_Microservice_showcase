package vn.edu.fpt.cinema.cinema.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.edu.fpt.cinema.cinema.entities.Showtime;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;

/** Physical layout of a showtime's room. Availability and price per showtime belong to booking-service. */
public record ShowtimeSeatMapResponse(
    String showtimeId,
    String movieId,
    String movieTitle,
    String roomId,
    String templateId,
    LocalDate showDate,
    LocalTime startTime,
    List<Seat> seats
) {
    public record Seat(String templateSeatId, String rowLabel, int seatNumber, String seatType) {}

    public static ShowtimeSeatMapResponse of(Showtime showtime, String movieTitle, String templateId, List<TemplateSeat> seats) {
        return new ShowtimeSeatMapResponse(
            showtime.getShowtimeId(),
            showtime.getMovieId(),
            movieTitle,
            showtime.getRoom() == null ? null : String.valueOf(showtime.getRoom().getId()),
            templateId,
            showtime.getShowDate(),
            showtime.getStartTime(),
            seats.stream().map(seat -> new Seat(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getSeatType() == null ? null : seat.getSeatType().name())).toList()
        );
    }
}
