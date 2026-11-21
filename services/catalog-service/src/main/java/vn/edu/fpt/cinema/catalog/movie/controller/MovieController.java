package vn.edu.fpt.cinema.catalog.movie.controller;

import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.entity.MovieStatus;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

// Public reads: guests may browse movies before signing in (AGENTS section 7).
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final MovieService service;
    public MovieController(MovieService service) { this.service = service; }
    @GetMapping
    public MoviePage list(@RequestParam(defaultValue="now-showing") String status,
                          @RequestParam(required=false) String search,
                          @RequestParam(defaultValue="0") int page,
                          @RequestParam(defaultValue="20") int size,
                          @RequestParam(required=false) String active) {
        if (active != null) throw new CatalogException(BAD_REQUEST,"VALIDATION_ERROR","Customer cannot override active filter");
        var schedule = MovieStatus.parse(status).orElseThrow(() ->
            new CatalogException(BAD_REQUEST,"VALIDATION_ERROR","Status must be now-showing or coming-soon"));
        return service.listPublic(schedule, search, page, size);
    }
    @GetMapping("/{id}")
    public MovieResponse detail(@PathVariable String id) { return service.detail(id, true); }
}
