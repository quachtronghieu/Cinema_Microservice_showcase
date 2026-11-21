package vn.edu.fpt.cinema.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Same body the Flutter app sends: {"outcome": "SUCCESS" | "FAILED"}. */
public record DemoPaymentRequest(@NotNull @Pattern(regexp = "SUCCESS|FAILED") String outcome) {}
