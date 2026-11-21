package vn.edu.fpt.cinemamanagement.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinemamanagement.entities.Booking;
import vn.edu.fpt.cinemamanagement.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    Booking findTopByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") String bookingId);

    @Query("""
            SELECT b.id FROM Booking b
            WHERE b.status = :status AND b.createdAt <= :expiresAt
            """)
    List<String> findIdsByStatusAndCreatedAtBefore(
            @Param("status") BookingStatus status,
            @Param("expiresAt") LocalDateTime expiresAt
    );
}
