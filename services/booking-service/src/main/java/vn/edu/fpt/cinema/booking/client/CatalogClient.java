package vn.edu.fpt.cinema.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Catalog's customer concession API, resolved through Eureka.
 * shortcut: forwards the customer's bearer token to a public catalog read; switch to the internal quote
 * protocol (service token + 60s quote, plan D3) once catalog publishes it.
 */
@FeignClient(name = "catalog-service")
public interface CatalogClient {
    @GetMapping("/api/v1/concessions/{id}")
    Concession concession(@RequestHeader("Authorization") String authorization, @PathVariable("id") String id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Concession(String concessionId, String name, BigDecimal price, boolean active) {}
}
