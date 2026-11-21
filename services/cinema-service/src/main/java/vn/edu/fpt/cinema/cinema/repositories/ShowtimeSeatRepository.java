package vn.edu.fpt.cinema.cinema.repositories;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.cinema.entities.ShowtimeSeat;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, String> {

    List<ShowtimeSeat> getAllByShowtime_ShowtimeId(String showtimeId);

    boolean existsByShowtime_ShowtimeId(String showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ss FROM ShowtimeSeat ss
        WHERE ss.showtime.showtimeId = :showtimeId
          AND ss.showtimeSeatID IN :showtimeSeatIds
    """)
    List<ShowtimeSeat> findByShowtimeIdAndIdsForUpdate(
            @Param("showtimeId") String showtimeId,
            @Param("showtimeSeatIds") List<String> showtimeSeatIds
    );
}

