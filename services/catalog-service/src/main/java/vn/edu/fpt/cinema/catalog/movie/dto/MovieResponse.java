package vn.edu.fpt.cinema.catalog.movie.dto;
import vn.edu.fpt.cinema.catalog.movie.entity.Movie;
import java.time.LocalDate;
public record MovieResponse(String movieId, String title, String genre, String summary, int duration, LocalDate releaseDate,
                            String ageRating, String img, String trailer, boolean active, long version) {
    public static MovieResponse from(Movie item) {
        return new MovieResponse(item.getMovieId(), item.getTitle(), item.getGenre(), item.getSummary(), item.getDuration(),
            item.getReleaseDate(), item.getAgeRating(), item.getImg(), item.getTrailer(), item.isActive(), item.getVersion());
    }
}
