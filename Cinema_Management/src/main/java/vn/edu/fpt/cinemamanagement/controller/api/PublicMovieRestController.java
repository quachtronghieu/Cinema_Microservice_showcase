package vn.edu.fpt.cinemamanagement.controller.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.cinemamanagement.dto.response.MovieResponseDTO;
import vn.edu.fpt.cinemamanagement.entities.Movie;
import vn.edu.fpt.cinemamanagement.services.MovieService;

@RestController
@RequestMapping("/api/public/movies")
public class PublicMovieRestController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/top")
    public ResponseEntity<?> getTopMovies() {
        List<Movie> top5Movies = movieService.getTop5Movies();

        List<Map<String, Object>> result = top5Movies.stream().map(movie -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("movieID", movie.getMovieID());
            map.put("title", movie.getTitle());
            map.put("genre", movie.getGenre());
            map.put("duration", movie.getDuration());
            map.put("img", movie.getImg() != null ? movie.getImg() : "");
            map.put("ageRating", movie.getAgeRating() != null ? movie.getAgeRating() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Danh sách phim cho khách (mobile), không cần đăng nhập.
     * status = now-showing: phát hành trong 30 ngày gần nhất, tính đến hôm nay (mới nhất trước).
     * status = coming-soon: phát hành sau hôm nay (sớm nhất trước).
     * Việc phân loại dựa trên ngày của server, client không tự tính.
     */
    @GetMapping
    public ResponseEntity<?> getMovies(@RequestParam(defaultValue = "now-showing") String status) {
        List<Movie> movies;
        switch (status) {
            case "now-showing" -> movies = movieService.getNowShowingMovies().stream()
                    .sorted(Comparator.comparing(Movie::getReleaseDate).reversed())
                    .toList();
            case "coming-soon" -> movies = movieService.getComingSoonMovies();
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "INVALID_STATUS",
                        "message", "status chỉ nhận now-showing hoặc coming-soon."
                ));
            }
        }
        return ResponseEntity.ok(movies.stream().map(MovieResponseDTO::fromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMovie(@PathVariable String id) {
        Movie movie = movieService.findById(id);
        if (movie == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", "NOT_FOUND",
                    "message", "Không tìm thấy phim."
            ));
        }
        return ResponseEntity.ok(MovieResponseDTO.fromEntity(movie));
    }
}