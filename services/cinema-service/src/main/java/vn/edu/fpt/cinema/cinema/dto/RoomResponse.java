package vn.edu.fpt.cinema.cinema.dto;

/** Room as seen by ADMIN. roomId is a string in the API although the key is INT (AGENTS 6). */
public record RoomResponse(String roomId, String templateId, String templateName, String status,
                           int capacity, int standardSeats, int vipSeats) {}
