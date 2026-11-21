package vn.edu.fpt.cinema.catalog.movie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Watchlist", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "movie_id"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_id")
    private Long id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "movie_id", length = 36, nullable = false)
    private String movieId;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public WatchlistItem() {}

    public WatchlistItem(String customerId, String movieId, LocalDateTime addedAt) {
        this.customerId = customerId;
        this.movieId = movieId;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getMovieId() { return movieId; }
    public LocalDateTime getAddedAt() { return addedAt; }
}

