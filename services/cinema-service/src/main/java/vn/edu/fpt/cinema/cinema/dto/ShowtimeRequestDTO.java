package vn.edu.fpt.cinema.cinema.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ShowtimeRequestDTO(
    String movieId,
    String roomId,
    LocalDate showDate,
    LocalTime startTime
) {}

