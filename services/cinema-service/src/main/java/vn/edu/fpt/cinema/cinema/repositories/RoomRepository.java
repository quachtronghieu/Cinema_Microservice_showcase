package vn.edu.fpt.cinema.cinema.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.cinema.entities.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    /**
     * Row lock held until commit. Status changes and scheduling into the room take this same lock,
     * so a room cannot go INACTIVE while a showtime is being placed in it (scope "Điều kiện đổi trạng thái phòng").
     */
    @Query(value = "SELECT status FROM dbo.Room WITH (UPDLOCK, ROWLOCK) WHERE room_id = :roomId", nativeQuery = true)
    Optional<String> lockStatus(@Param("roomId") Integer roomId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE dbo.Room SET status = CAST(:status AS VARCHAR(20)) WHERE room_id = :roomId", nativeQuery = true)
    int updateStatus(@Param("roomId") Integer roomId, @Param("status") String status);

    @Modifying
    @Query(value = "INSERT INTO dbo.Room_Status_Audit (room_id, old_status, new_status, actor_id, trace_id) "
        + "VALUES (:roomId, CAST(:oldStatus AS VARCHAR(20)), CAST(:newStatus AS VARCHAR(20)), "
        + "CAST(:actorId AS VARCHAR(64)), CAST(:traceId AS VARCHAR(64)))", nativeQuery = true)
    int insertAudit(@Param("roomId") Integer roomId, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus,
                    @Param("actorId") String actorId, @Param("traceId") String traceId);
}
