package vn.edu.fpt.cinema.catalog.movie.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import vn.edu.fpt.cinema.catalog.movie.validation.MovieImages;
import java.time.Duration;

// Legacy posters are static classpath files under /assets/img/movies/. Only uploads pass through here.
@RestController
public class MovieImageController {
    private final MovieService service;
    public MovieImageController(MovieService service) { this.service = service; }
    @GetMapping(MovieImages.UPLOAD_PATH + "{imageId}.{extension}")
    public ResponseEntity<byte[]> image(@PathVariable String imageId, @PathVariable String extension) {
        var image = service.image(imageId);
        if (!image.getContentType().equals(MovieImages.mediaTypeFor(extension)))
            throw new CatalogException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Image not found");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getContentType()))
            // An uploaded image never changes: a new upload always receives a new ID.
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(image.getData());
    }
}
