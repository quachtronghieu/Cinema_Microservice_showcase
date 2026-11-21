package vn.edu.fpt.cinema.identity.application;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.dto.CustomerProfileDTO;
import vn.edu.fpt.cinema.identity.dto.CustomerProfileUpdateRequest;
import vn.edu.fpt.cinema.identity.repository.CustomerRepository;

@Service
public class CustomerProfileService {

    private final CustomerRepository customerRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9]{9,11}$"
    );

    public CustomerProfileService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerProfileDTO getProfileByUserId(String userId) {
        Customer customer = customerRepository.findById(userId).orElse(null);
        if (customer == null) {
            return null;
        }
        return CustomerProfileDTO.fromEntity(customer);
    }

    public CustomerProfileDTO getProfileByUsername(String username) {
        Customer customer = customerRepository.findByUsername(username).orElse(null);
        if (customer == null) {
            return null;
        }
        return CustomerProfileDTO.fromEntity(customer);
    }

    @Transactional
    public Map<String, String> updateProfile(String userId, CustomerProfileUpdateRequest request) {
        Customer customer = customerRepository.findById(userId).orElse(null);
        if (customer == null) {
            return Map.of("user", "User not found");
        }

        Map<String, String> errors = new HashMap<>();

        // 1. Validate Date of Birth
        if (request.dob() == null) {
            errors.put("dob", "Date of birth is required.");
        } else {
            LocalDate today = LocalDate.now();
            if (request.dob().isAfter(today)) {
                errors.put("dob", "The date of birth cannot be later than today.");
            } else {
                int currentYear = today.getYear();
                int birthYear = request.dob().getYear();
                if (birthYear < 1900 || birthYear > currentYear) {
                    errors.put("dob", String.format("The year of birth must be between 1900–%d", currentYear));
                } else if (currentYear - birthYear < 10) {
                    errors.put("dob", "You must be at least 10 years old.");
                }
            }
        }

        // 2. Validate Email
        if (request.email() == null || request.email().trim().isEmpty()) {
            errors.put("email", "Email is required.");
        } else if (!EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            errors.put("email", "Invalid email.");
        } else {
            customerRepository.findByEmail(request.email().trim()).ifPresent(existing -> {
                if (!existing.getUserId().equals(userId)) {
                    errors.put("email", "Email already exists");
                }
            });
        }

        // 3. Validate Phone
        if (request.phone() == null || request.phone().trim().isEmpty()) {
            errors.put("phone", "The phone number is required.");
        } else if (!PHONE_PATTERN.matcher(request.phone().trim()).matches()) {
            errors.put("phone", "The phone number must have 9-11 digits.");
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        // Update fields
        customer.setDob(request.dob());
        customer.setSex(request.sex());
        customer.setEmail(request.email().trim());
        customer.setPhone(request.phone().trim());

        customerRepository.save(customer);
        return Map.of();
    }
}

