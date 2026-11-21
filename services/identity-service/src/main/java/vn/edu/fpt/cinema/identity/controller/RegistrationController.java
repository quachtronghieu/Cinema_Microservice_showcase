package vn.edu.fpt.cinema.identity.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinema.identity.application.CustomerRegistrationService;
import vn.edu.fpt.cinema.identity.application.CustomerRegistrationService.RegistrationValidationException;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.dto.RegisterRequest;
import vn.edu.fpt.cinema.identity.dto.RegisterResponse;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class RegistrationController {

    private final CustomerRegistrationService registrationService;

    public RegistrationController(CustomerRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Customer registration endpoint.
     * Dual-routed to /api/v1/auth/register (microservice standard) and /api/auth/register (Flutter mobile app).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Customer customer = registrationService.register(request);
        RegisterResponse response = new RegisterResponse(
                "Registration successful.",
                customer.getUserId(),
                customer.getUsername(),
                customer.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(RegistrationValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(RegistrationValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "VALIDATION_ERROR");
        body.put("message", "Invalid registration request");
        body.put("fieldErrors", ex.getErrors());
        body.put("errors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
