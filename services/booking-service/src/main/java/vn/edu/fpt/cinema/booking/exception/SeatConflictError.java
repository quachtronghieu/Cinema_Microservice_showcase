package vn.edu.fpt.cinema.booking.exception;

import java.util.List;
import java.util.Map;

/** The common error shape (AGENTS 6) plus the conflicting seats allowed by AGENTS 18.1. */
public record SeatConflictError(String code, String message, String traceId, Map<String, List<String>> fieldErrors, List<String> seatIds) {}
