package vn.edu.fpt.cinema.identity.dto;

import java.time.LocalDate;

public record CustomerProfileUpdateRequest(
    String phone,
    String email,
    LocalDate dob,
    Boolean sex
) {}

