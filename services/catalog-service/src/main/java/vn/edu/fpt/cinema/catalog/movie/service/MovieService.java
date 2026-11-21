package vn.edu.fpt.cinema.catalog.movie.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.entity.*;
import vn.edu.fpt.cinema.catalog.movie.repository.*;
import vn.edu.fpt.cinema.catalog.movie.validation.MovieImages;
import java.time.*;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly = true)
public class MovieService {
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int NOW_SHOWING_DAYS = 30;
    private final MovieRepository repository;
    private final MovieImageRepository images;
    private final Clock clock;
    public MovieService(MovieRepository repository, MovieImageRepository images, Clock clock) {
        this.repository = repository; this.images = images; this.clock = clock;
    }

    public MoviePage listPublic(MovieStatus status, String search, int page, int size) {
        LocalDate today = LocalDate.now(clock);
        Specification<Movie> filter = (root, query, cb) -> cb.isTrue(root.get("active"));
        Sort sort;
        if (status == MovieStatus.NOW_SHOWING) {
            filter = filter.and((root, query, cb) -> cb.between(root.get("releaseDate"), today.minusDays(NOW_SHOWING_DAYS), today));
            sort = Sort.by(Sort.Order.desc("releaseDate"), Sort.Order.asc("movieId"));
        } else {
            filter = filter.and((root, query, cb) -> cb.greaterThan(root.get("releaseDate"), today));
            sort = Sort.by(Sort.Order.asc("releaseDate"), Sort.Order.asc("movieId"));
        }
        return list(filter, search, page, size, sort);
    }
    public MoviePage listAdmin(String search, Boolean active, int page, int size) {
        Specification<Movie> filter = (root, query, cb) -> cb.conjunction();
        if (active != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return list(filter, search, page, size, Sort.by(Sort.Order.desc("releaseDate"), Sort.Order.asc("movieId")));
    }
    public MovieResponse detail(String id, boolean customer) {
        var item = repository.findById(id).orElseThrow(MovieService::notFound);
        if (customer && !item.isActive()) throw notFound();
        return MovieResponse.from(item);
    }
    public MovieImage image(String imageId) {
        return images.findById(imageId).orElseThrow(() -> new CatalogException(NOT_FOUND, "IMAGE_NOT_FOUND", "Image not found"));
    }
    @Transactional
    public MovieResponse create(CreateMovieRequest request, MultipartFile image) {
        if (image == null || image.isEmpty()) throw MovieImages.invalid("Movie poster image is required");
        var content = MovieImages.read(image);
        checkNewReleaseDate(request.releaseDate());
        String title = normalizeTitle(request.title());
        if (repository.existsByTitleIgnoreCaseAndActiveTrue(title)) throw titleConflict();
        var item = new Movie(UUID.randomUUID().toString(), title, request.genre(), request.summary(), request.duration(),
            request.releaseDate(), request.ageRating(), store(content), request.trailer());
        try { return MovieResponse.from(repository.saveAndFlush(item)); }
        catch (DataIntegrityViolationException e) { throw translate(e); }
    }
    @Transactional
    public MovieResponse update(String id, UpdateMovieRequest request, MultipartFile image) {
        var content = image == null || image.isEmpty() ? null : MovieImages.read(image);
        var item = repository.findForUpdate(id).orElseThrow(MovieService::notFound);
        checkVersion(item, request.version()); // Must precede data equality.
        if (!request.releaseDate().equals(item.getReleaseDate())) checkChangedReleaseDate(item.getReleaseDate(), request.releaseDate());
        String title = normalizeTitle(request.title());
        if (item.isActive() && !title.equalsIgnoreCase(item.getTitle())
                && repository.existsByTitleIgnoreCaseAndActiveTrueAndMovieIdNot(title, id)) throw titleConflict();
        // The previous poster is kept: Booking snapshots may still reference its URL.
        String img = content == null ? item.getImg() : store(content);
        if (!item.matches(title, request.genre(), request.summary(), request.duration(), request.releaseDate(),
                request.ageRating(), img, request.trailer())) {
            item.update(title, request.genre(), request.summary(), request.duration(), request.releaseDate(),
                request.ageRating(), img, request.trailer());
            try { repository.flush(); } // @Version increments only for a dirty managed entity.
            catch (DataIntegrityViolationException e) { throw translate(e); }
        }
        return MovieResponse.from(item);
    }
    // Soft delete only: Cinema and Booking keep referring to this movie's ID.
    @Transactional
    public void deactivate(String id, long version) {
        if (version < 0) throw new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid version");
        var item = repository.findForUpdate(id).orElseThrow(MovieService::notFound);
        if (!item.isActive()) return;
        checkVersion(item, version);
        item.deactivate();
        repository.flush();
    }
    private MoviePage list(Specification<Movie> filter, String search, int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100)
            throw new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid pagination");
        return MoviePage.from(repository.findAll(filter.and(titleContains(search)), PageRequest.of(page, size, sort))
            .map(MovieResponse::from));
    }
    private static Specification<Movie> titleContains(String search) {
        String needle = search == null ? "" : search.trim();
        if (needle.length() > MAX_SEARCH_LENGTH)
            throw new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Search must not exceed 100 characters");
        if (needle.isEmpty()) return (root, query, cb) -> cb.conjunction();
        String pattern = "%" + escapeLike(needle.toUpperCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get("title")), pattern, '\\');
    }
    // SQL Server also treats [ as a wildcard, so % and _ alone are not enough.
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
    }
    private String store(MovieImages.Content content) {
        String imageId = UUID.randomUUID().toString();
        images.save(new MovieImage(imageId, content.mediaType(), content.data()));
        return MovieImages.publicPath(imageId, content.extension());
    }
    private void checkNewReleaseDate(LocalDate releaseDate) {
        LocalDate today = LocalDate.now(clock);
        if (releaseDate.isBefore(today.plusWeeks(2))) throw invalidDate("Release date must be at least 2 weeks after today");
        if (releaseDate.isAfter(today.plusMonths(5))) throw invalidDate("Release date cannot be more than 5 months from today");
    }
    private void checkChangedReleaseDate(LocalDate current, LocalDate requested) {
        LocalDate today = LocalDate.now(clock);
        if (!today.isBefore(current)) throw invalidDate("Release date cannot change after the movie has been released");
        if (!requested.isAfter(today)) throw invalidDate("Release date must be after today");
        if (requested.isAfter(today.plusMonths(5))) throw invalidDate("Release date cannot be more than 5 months from today");
    }
    private static String normalizeTitle(String title) { return title.trim().toUpperCase(Locale.ROOT); }
    private static void checkVersion(Movie item, long version) {
        if (item.getVersion() != version)
            throw new CatalogException(CONFLICT, "MOVIE_VERSION_CONFLICT", "Movie changed. Read it again before updating.");
    }
    private static CatalogException translate(DataIntegrityViolationException e) {
        // Two concurrent writers can both pass the title check; the filtered unique index decides.
        var cause = e.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains("UX_Movie_active_title")) return titleConflict();
        throw e;
    }
    private static CatalogException titleConflict() {
        return new CatalogException(CONFLICT, "MOVIE_TITLE_CONFLICT", "A movie with this title already exists",
            Map.of("title", List.of("A movie with this title already exists")));
    }
    private static CatalogException invalidDate(String message) {
        return new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid movie data", Map.of("releaseDate", List.of(message)));
    }
    private static CatalogException notFound() {
        return new CatalogException(NOT_FOUND, "MOVIE_NOT_FOUND", "Movie not found");
    }
}
