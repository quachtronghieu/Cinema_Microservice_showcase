package vn.edu.fpt.cinema.cinema.dto;

import java.util.List;

/** Fixed seat layout of a room. It is read-only: there is no API to change rooms or seats. */
public record RoomTemplateResponse(String roomId, String templateId, String templateName, String description,
                                   int capacity, List<Seat> seats) {
    public record Seat(String templateSeatId, String rowLabel, int seatNumber, String seatType) {}
}
