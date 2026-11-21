package vn.edu.fpt.cinema.booking.dto;

import java.time.Instant;

public record CheckInResponse(String ticketId, Instant checkedInAt, String checkedInBy) {}
