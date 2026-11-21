package vn.edu.fpt.cinema.cinema.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.cinema.entities.Showtime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, String> {

    Showtime findByShowtimeId(String showtimeId);

    List<Showtime> findByMovieId(String movieId);

    List<Showtime> findByShowDate(LocalDate showDate);

    List<Showtime> findByMovieIdAndShowDateOrderByStartTimeAsc(String movieId, LocalDate showDate);

    List<Showtime> findByRoom_IdAndShowDate(Integer roomId, LocalDate showDate);

    List<Showtime> findByRoom_IdAndShowDateBetween(Integer roomId, LocalDate fromDate, LocalDate toDate);

    /** Every showtime of the room from this date on, with no upper bound (scope: never cut at 7 or 30 days). */
    List<Showtime> findByRoom_IdAndShowDateGreaterThanEqual(Integer roomId, LocalDate fromDate);

    @Query("""
        SELECT s FROM Showtime s
        WHERE s.room.id = :roomId
          AND s.showDate = :showDate
          AND ((s.startTime < :endTime AND s.endTime > :startTime))
    """)
    List<Showtime> findConflictingShowtimes(
            @Param("roomId") Integer roomId,
            @Param("showDate") LocalDate showDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}

