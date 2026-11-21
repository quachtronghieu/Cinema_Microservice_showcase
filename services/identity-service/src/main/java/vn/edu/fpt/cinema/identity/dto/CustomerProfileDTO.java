package vn.edu.fpt.cinema.identity.dto;

import java.time.LocalDate;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;

public record CustomerProfileDTO(
    String userId,
    String username,
    String email,
    String phone,
    LocalDate dob,
    Boolean sex
) {
    public static CustomerProfileDTO fromEntity(Customer customer) {
        return new CustomerProfileDTO(
            customer.getUserId(),
            customer.getUsername(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getDob(),
            customer.getSex()
        );
    }
}

