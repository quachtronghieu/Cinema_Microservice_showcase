package vn.edu.fpt.cinema.catalog.concession.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.ConcessionType;
import vn.edu.fpt.cinema.catalog.concession.service.ConcessionService;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/concessions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConcessionController {
    private final ConcessionService service;
    public AdminConcessionController(ConcessionService service) { this.service = service; }
    @GetMapping
    public ConcessionPage list(@RequestParam(required=false) ConcessionType type,
                               @RequestParam(required=false) String active,
                               @RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int size) {
        if (active != null && !active.equals("true") && !active.equals("false"))
            throw new vn.edu.fpt.cinema.catalog.exception.CatalogException(
                org.springframework.http.HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid active filter");
        return service.list(type, active == null ? null : Boolean.valueOf(active), page, size);
    }
    @GetMapping("/{id}")
    public ConcessionResponse detail(@PathVariable String id) { return service.detail(id, false); }
    @PostMapping
    public ResponseEntity<ConcessionResponse> create(@Valid @RequestBody CreateConcessionRequest request) {
        var response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/concessions/" + response.concessionId())).body(response);
    }
    @PutMapping("/{id}")
    public ConcessionResponse update(@PathVariable String id, @Valid @RequestBody UpdateConcessionRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam long version) {
        if (version < 0) throw new vn.edu.fpt.cinema.catalog.exception.CatalogException(
            org.springframework.http.HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid version");
        service.deactivate(id, version);
        return ResponseEntity.noContent().build();
    }
}
