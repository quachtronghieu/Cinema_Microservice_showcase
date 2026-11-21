package vn.edu.fpt.cinema.catalog.movie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Movie", schema = "dbo")
public class Movie {
    @Id @Column(name = "movie_id", length = 36, nullable = false)
    private String movieId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false) private int duration;
    @Column(nullable = false, length = 100) private String genre;
    @Column(name = "release_date", nullable = false) private LocalDate releaseDate;
    @Column(columnDefinition = "varchar(max)") private String summary;
    @Column(name = "age_rating", length = 10) private String ageRating;
    @Column(nullable = false, length = 255) private String img;
    @Column(nullable = false, length = 255) private String trailer;
    @Column(nullable = false) private boolean active;
    @Version @Column(nullable = false) private Long version;

    protected Movie() {}
    public Movie(String id, String title, String genre, String summary, int duration, LocalDate releaseDate,
                 String ageRating, String img, String trailer) {
        this.movieId = id; this.title = title; this.genre = genre; this.summary = summary; this.duration = duration;
        this.releaseDate = releaseDate; this.ageRating = ageRating; this.img = img; this.trailer = trailer; this.active = true;
    }
    public boolean matches(String title, String genre, String summary, int duration, LocalDate releaseDate,
                           String ageRating, String img, String trailer) {
        return this.title.equals(title) && this.genre.equals(genre) && summary.equals(this.summary)
                && this.duration == duration && this.releaseDate.equals(releaseDate)
                && ageRating.equals(this.ageRating) && this.img.equals(img) && this.trailer.equals(trailer);
    }
    public void update(String title, String genre, String summary, int duration, LocalDate releaseDate,
                       String ageRating, String img, String trailer) {
        this.title = title; this.genre = genre; this.summary = summary; this.duration = duration;
        this.releaseDate = releaseDate; this.ageRating = ageRating; this.img = img; this.trailer = trailer;
    }
    public void deactivate() { this.active = false; }
    public String getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public int getDuration() { return duration; }
    public String getGenre() { return genre; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getSummary() { return summary; }
    public String getAgeRating() { return ageRating; }
    public String getImg() { return img; }
    public String getTrailer() { return trailer; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
}
