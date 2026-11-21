package vn.edu.fpt.cinema.booking.dto;

import java.util.List;

/** List contract from AGENTS 23: page starts at 0, size at most 100. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
