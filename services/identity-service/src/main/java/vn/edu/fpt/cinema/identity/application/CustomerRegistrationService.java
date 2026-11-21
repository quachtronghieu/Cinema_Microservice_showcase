package vn.edu.fpt.cinema.identity.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.entity.PrincipalRole;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;
import vn.edu.fpt.cinema.identity.dto.RegisterRequest;
import vn.edu.fpt.cinema.identity.repository.CustomerRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRoleRepository;

@Service
public class CustomerRegistrationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,11}$");

    private final CustomerRepository customerRepository;
    private final PrincipalRepository principalRepository;
    private final PrincipalRoleRepository principalRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerRegistrationService(
            CustomerRepository customerRepository,
            PrincipalRepository principalRepository,
            PrincipalRoleRepository principalRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.principalRepository = principalRepository;
        this.principalRoleRepository = principalRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates input, checks uniqueness, encodes password with BCrypt,
     * and persists Customer, Principal, and Role in a single atomic transaction.
     */
    @Transactional
    public Customer register(RegisterRequest request) {
        Map<String, String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException(errors);
        }

        String rawUsername = request.getUsername().trim();
        String normalizedUsername = rawUsername.toLowerCase();
        String customerId = UUID.randomUUID().toString();
        String principalId = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        LocalDate parsedDob = parseDate(request.getDob());

        // 1. Create and persist Customer
        Customer customer = new Customer();
        customer.setUserId(customerId);
        customer.setUsername(rawUsername);
        customer.setPasswordHash(encodedPassword);
        customer.setDob(parsedDob);
        customer.setSex(request.getSex());
        customer.setEmail(request.getEmail().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setVerify("active");
        Customer savedCustomer = customerRepository.save(customer);

        // 2. Create and persist Unified Principal for authentication
        Principal principal = new Principal();
        principal.setPrincipalId(principalId);
        principal.setUsername(rawUsername);
        principal.setNormalizedUsername(normalizedUsername);
        principal.setPasswordHash(encodedPassword);
        principal.setAccountType(AccountType.CUSTOMER);
        principal.setCustomerId(customerId);
        principal.setIsActive(true);

        // 3. Assign Default Role: ROLE_USER
        PrincipalRole role = new PrincipalRole();
        role.setPrincipalRoleId(UUID.randomUUID().toString());
        role.setPrincipal(principal);
        role.setRoleName(RoleType.USER);
        principal.getRoles().add(role);

        principalRepository.save(principal);

        return savedCustomer;
    }

    public Map<String, String> validate(RegisterRequest request) {
        Map<String, String> errors = new HashMap<>();

        // 1. Format validation
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            errors.put("username", "The username is required.");
        } else {
            String u = request.getUsername().trim();
            if (u.length() < 2 || u.length() > 20) {
                errors.put("username", "The name must have between 2 and 20 characters.");
            } else if (u.contains(" ")) {
                errors.put("username", "Usernames cannot contain spaces.");
            } else if (!USERNAME_PATTERN.matcher(u).matches()) {
                errors.put("username", "Usernames can only include letters, numbers, or underscores.");
            }
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            errors.put("password", "A password is required.");
        } else if (request.getPassword().length() < 6) {
            errors.put("password", "The password must be at least 6 characters long.");
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().isEmpty()) {
            errors.put("confirmPassword", "Confirmation password is required.");
        } else if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            errors.put("confirmPassword", "The password and confirmation password do not match.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            errors.put("email", "Email is required.");
        } else if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            errors.put("email", "Invalid email.");
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.put("phone", "The phone number is required.");
        } else if (!PHONE_PATTERN.matcher(request.getPhone().trim()).matches()) {
            errors.put("phone", "The phone number must have 9-11 digits.");
        }

        if (request.getSex() == null) {
            errors.put("sex", "Please select a gender.");
        }

        LocalDate dob = parseDate(request.getDob());
        if (dob == null) {
            errors.put("dob", "Date of birth is required and must be valid (YYYY-MM-DD).");
        } else {
            LocalDate today = LocalDate.now();
            if (dob.isAfter(today)) {
                errors.put("dob", "The date of birth cannot be later than today.");
            } else if (dob.getYear() < 1900 || dob.getYear() > today.getYear()) {
                errors.put("dob", String.format("The year of birth must be between 1900–%d.", today.getYear()));
            } else if (Period.between(dob, today).getYears() < 10) {
                errors.put("dob", "You must be at least 10 years old.");
            }
        }

        // Return early on format errors before hitting DB
        if (!errors.isEmpty()) {
            return errors;
        }

        // 2. Business rules / uniqueness checks
        String normalized = request.getUsername().trim().toLowerCase();
        if (principalRepository.existsByNormalizedUsername(normalized) || customerRepository.existsByUsername(request.getUsername().trim())) {
            errors.put("username", "Customer with username " + request.getUsername().trim() + " already exists");
        }

        if (customerRepository.existsByEmail(request.getEmail().trim())) {
            errors.put("email", "Customer with email " + request.getEmail().trim() + " already exists");
        }

        if (customerRepository.existsByPhone(request.getPhone().trim())) {
            errors.put("phone", "Customer with phone " + request.getPhone().trim() + " already exists");
        }

        return errors;
    }

    private LocalDate parseDate(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dobStr.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    public static class RegistrationValidationException extends RuntimeException {
        private final Map<String, String> errors;

        public RegistrationValidationException(Map<String, String> errors) {
            super("Validation failed for customer registration");
            this.errors = errors;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}
