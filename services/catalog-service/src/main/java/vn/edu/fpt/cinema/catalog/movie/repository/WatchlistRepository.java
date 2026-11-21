package vn.edu.fpt.cinema.catalog.movie.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.catalog.movie.entity.WatchlistItem;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByCustomerIdOrderByAddedAtDesc(String customerId);
    boolean existsByCustomerIdAndMovieId(String customerId, String movieId);
    void deleteByCustomerIdAndMovieId(String customerId, String movieId);
}

