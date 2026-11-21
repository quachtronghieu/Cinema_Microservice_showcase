package vn.edu.fpt.cinema.catalog.movie.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.catalog.movie.dto.MovieResponse;
import vn.edu.fpt.cinema.catalog.movie.entity.Movie;
import vn.edu.fpt.cinema.catalog.movie.entity.WatchlistItem;
import vn.edu.fpt.cinema.catalog.movie.repository.MovieRepository;
import vn.edu.fpt.cinema.catalog.movie.repository.WatchlistRepository;

@Service
public class CustomerWatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MovieRepository movieRepository;

    public CustomerWatchlistService(WatchlistRepository watchlistRepository, MovieRepository movieRepository) {
        this.watchlistRepository = watchlistRepository;
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> list(String customerId) {
        List<String> movieIds = watchlistRepository.findByCustomerIdOrderByAddedAtDesc(customerId).stream()
                .map(WatchlistItem::getMovieId)
                .toList();

        // A soft-deleted movie stays saved but is hidden from customers everywhere.
        return movieRepository.findAllById(movieIds).stream()
                .filter(Movie::isActive)
                .map(MovieResponse::from)
                .toList();
    }

    @Transactional
    public boolean add(String customerId, String movieId) {
        if (!movieRepository.existsByMovieIdAndActiveTrue(movieId)) {
            return false;
        }
        if (!watchlistRepository.existsByCustomerIdAndMovieId(customerId, movieId)) {
            watchlistRepository.save(new WatchlistItem(customerId, movieId, LocalDateTime.now()));
        }
        return true;
    }

    @Transactional
    public void remove(String customerId, String movieId) {
        watchlistRepository.deleteByCustomerIdAndMovieId(customerId, movieId);
    }
}

