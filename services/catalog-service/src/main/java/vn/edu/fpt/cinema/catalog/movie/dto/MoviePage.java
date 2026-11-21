package vn.edu.fpt.cinema.catalog.movie.dto;
import org.springframework.data.domain.Page;
import java.util.List;
public record MoviePage(List<MovieResponse> items, int page, int size, long totalElements, int totalPages) {
    public static MoviePage from(Page<MovieResponse> page) {
        return new MoviePage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
