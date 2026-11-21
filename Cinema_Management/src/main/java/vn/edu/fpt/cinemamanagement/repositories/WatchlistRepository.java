package vn.edu.fpt.cinemamanagement.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinemamanagement.entities.WatchlistItem;
import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByCustomerIdOrderByAddedAtDesc(String customerId);
    boolean existsByCustomerIdAndMovieId(String customerId, String movieId);
    long deleteByCustomerIdAndMovieId(String customerId, String movieId);
}
