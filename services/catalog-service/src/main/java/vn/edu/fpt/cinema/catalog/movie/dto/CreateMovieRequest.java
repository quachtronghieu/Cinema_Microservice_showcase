package vn.edu.fpt.cinema.catalog.movie.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record CreateMovieRequest(
    @NotBlank @Size(min=2, max=100) @Pattern(regexp=MovieRules.TITLE, message=MovieRules.TITLE_MESSAGE) String title,
    @NotBlank @Size(min=2, max=100) @Pattern(regexp=MovieRules.GENRE, message=MovieRules.GENRE_MESSAGE) String genre,
    @NotBlank @Size(min=10, max=1000) @Pattern(regexp=MovieRules.SUMMARY, message=MovieRules.SUMMARY_MESSAGE) String summary,
    @NotNull @Min(value=MovieRules.MIN_DURATION, message=MovieRules.DURATION_MESSAGE)
    @Max(value=MovieRules.MAX_DURATION, message=MovieRules.DURATION_MESSAGE) Integer duration,
    @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate releaseDate,
    @NotBlank @Pattern(regexp=MovieRules.AGE_RATING, message=MovieRules.AGE_RATING_MESSAGE) String ageRating,
    @NotBlank @Size(max=255) @Pattern(regexp=MovieRules.TRAILER, message=MovieRules.TRAILER_MESSAGE) String trailer
) {
    public CreateMovieRequest {
        title = title == null ? null : title.trim();
        genre = genre == null ? null : genre.trim();
        summary = summary == null ? null : summary.trim();
        trailer = trailer == null ? null : trailer.trim();
    }
}
