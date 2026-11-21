package vn.edu.fpt.cinema.catalog.movie.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {
    private static final Set<String> CREATE_FIELDS =
        Set.of("title","genre","summary","duration","releaseDate","ageRating","trailer");
    private static final Set<String> UPDATE_FIELDS =
        Set.of("title","genre","summary","duration","releaseDate","ageRating","trailer","version");
    private final MovieService service;
    public AdminMovieController(MovieService service) { this.service = service; }
    @GetMapping
    public MoviePage list(@RequestParam(required=false) String search,
                          @RequestParam(required=false) String active,
                          @RequestParam(defaultValue="0") int page,
                          @RequestParam(defaultValue="20") int size) {
        if (active != null && !active.equals("true") && !active.equals("false"))
            throw new CatalogException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid active filter");
        return service.listAdmin(search, active == null ? null : Boolean.valueOf(active), page, size);
    }
    @GetMapping("/{id}")
    public MovieResponse detail(@PathVariable String id) { return service.detail(id, false); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovieResponse> create(@Valid @ModelAttribute CreateMovieRequest request,
                                                @RequestPart(value="image", required=false) MultipartFile image,
                                                HttpServletRequest servletRequest) {
        rejectUnknownFields(servletRequest, CREATE_FIELDS);
        var response = service.create(request, image);
        return ResponseEntity.created(URI.create("/api/v1/admin/movies/" + response.movieId())).body(response);
    }
    @PutMapping(value="/{id}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public MovieResponse update(@PathVariable String id, @Valid @ModelAttribute UpdateMovieRequest request,
                                @RequestPart(value="image", required=false) MultipartFile image,
                                HttpServletRequest servletRequest) {
        rejectUnknownFields(servletRequest, UPDATE_FIELDS);
        return service.update(id, request, image);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam long version) {
        if (version < 0) throw new CatalogException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid version");
        service.deactivate(id, version);
        return ResponseEntity.noContent().build();
    }
    // Form binding would silently drop server-owned fields such as active or movieId.
    private static void rejectUnknownFields(HttpServletRequest request, Set<String> allowed) {
        for (String field : request.getParameterMap().keySet())
            if (!allowed.contains(field)) throw new CatalogException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid movie data",
                Map.of(field, List.of("Unknown field")));
    }
}
