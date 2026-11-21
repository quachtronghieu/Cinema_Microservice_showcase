package vn.edu.fpt.cinemamanagement.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinemamanagement.repositories.CustomerRepository;
import vn.edu.fpt.cinemamanagement.services.CustomerTicketService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/tickets")
public class CustomerTicketRestController {
    private final CustomerTicketService ticketService;
    private final CustomerRepository customerRepository;

    public CustomerTicketRestController(
            CustomerTicketService ticketService,
            CustomerRepository customerRepository
    ) {
        this.ticketService = ticketService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<?> findAll(Principal principal) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        return ResponseEntity.ok(ticketService.findAllForCustomer(customerId));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<?> findOne(Principal principal, @PathVariable String ticketId) {
        String customerId = customerId(principal);
        if (customerId == null) return unauthorized();
        try {
            return ResponseEntity.ok(ticketService.findForCustomer(customerId, ticketId));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (java.util.NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private String customerId(Principal principal) {
        if (principal == null) return null;
        return customerRepository.findByUsername(principal.getName())
                .map(customer -> customer.getUser_id())
                .orElse(null);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("message", "Tài khoản khách hàng không hợp lệ."));
    }
}
