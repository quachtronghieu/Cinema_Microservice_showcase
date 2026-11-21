package vn.edu.fpt.cinema.catalog.concession.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.ConcessionType;
import vn.edu.fpt.cinema.catalog.concession.service.ConcessionService;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/concessions")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class CustomerConcessionController {
    private final ConcessionService service;
    public CustomerConcessionController(ConcessionService service) { this.service = service; }
    @GetMapping
    public ConcessionPage list(@RequestParam(required=false) ConcessionType type,
                               @RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int size,
                               @RequestParam(required=false) String active) {
        if (active != null) throw new CatalogException(BAD_REQUEST,"VALIDATION_ERROR","Customer cannot override active filter");
        return service.list(type, true, page, size);
    }
    @GetMapping("/{id}")
    public ConcessionResponse detail(@PathVariable String id) { return service.detail(id, true); }
}
