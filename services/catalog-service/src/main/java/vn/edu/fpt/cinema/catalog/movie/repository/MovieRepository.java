package vn.edu.fpt.cinema.catalog.movie.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.cinema.catalog.movie.entity.Movie;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie,String>, JpaSpecificationExecutor<Movie> {
    // PUT, DELETE and no-op version checks serialize on the same database row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movie m where m.movieId = :id")
    Optional<Movie> findForUpdate(@Param("id") String id);
    boolean existsByMovieIdAndActiveTrue(String movieId);
    boolean existsByTitleIgnoreCaseAndActiveTrue(String title);
    boolean existsByTitleIgnoreCaseAndActiveTrueAndMovieIdNot(String title, String movieId);
}
