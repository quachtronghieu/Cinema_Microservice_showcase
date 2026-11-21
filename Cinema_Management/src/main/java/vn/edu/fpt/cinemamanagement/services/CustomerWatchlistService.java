package vn.edu.fpt.cinemamanagement.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinemamanagement.entities.Movie;
import vn.edu.fpt.cinemamanagement.entities.WatchlistItem;
import vn.edu.fpt.cinemamanagement.repositories.MovieRepository;
import vn.edu.fpt.cinemamanagement.repositories.WatchlistRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerWatchlistService {
    private final WatchlistRepository watchlistRepository;
    private final MovieRepository movieRepository;

    public CustomerWatchlistService(WatchlistRepository watchlistRepository, MovieRepository movieRepository) {
        this.watchlistRepository = watchlistRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public List<Movie> list(String customerId) {
        List<WatchlistItem> items = watchlistRepository.findByCustomerIdOrderByAddedAtDesc(customerId);
        Map<String, Movie> byId = movieRepository
                .findAllById(items.stream().map(WatchlistItem::getMovieId).toList())
                .stream()
                .collect(Collectors.toMap(Movie::getMovieID, Function.identity()));
        return items.stream().map(i -> byId.get(i.getMovieId())).filter(Objects::nonNull).toList();
    }

    /** @return false nếu phim không tồn tại; thêm lặp lại là no-op. */
    @Transactional
    public boolean add(String customerId, String movieId) {
        if (!movieRepository.existsById(movieId)) return false;
        if (watchlistRepository.existsByCustomerIdAndMovieId(customerId, movieId)) return true;
        try {
            watchlistRepository.saveAndFlush(new WatchlistItem(customerId, movieId, LocalDateTime.now()));
        } catch (DataIntegrityViolationException duplicate) {
            // Hai request cùng lúc: ràng buộc unique đã giữ đúng một dòng.
        }
        return true;
    }

    @Transactional
    public void remove(String customerId, String movieId) {
        watchlistRepository.deleteByCustomerIdAndMovieId(customerId, movieId);
    }
}
