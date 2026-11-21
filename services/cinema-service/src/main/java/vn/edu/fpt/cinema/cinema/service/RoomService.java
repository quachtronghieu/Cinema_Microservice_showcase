package vn.edu.fpt.cinema.cinema.service;

import static org.springframework.http.HttpStatus.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.cinema.dto.PageResponse;
import vn.edu.fpt.cinema.cinema.dto.RoomResponse;
import vn.edu.fpt.cinema.cinema.dto.RoomTemplateResponse;
import vn.edu.fpt.cinema.cinema.entities.Room;
import vn.edu.fpt.cinema.cinema.entities.Showtime;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;
import vn.edu.fpt.cinema.cinema.enums.SeatType;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;
import vn.edu.fpt.cinema.cinema.repositories.RoomRepository;
import vn.edu.fpt.cinema.cinema.repositories.ShowtimeRepository;

/**
 * Rooms and their layouts are fixed (docs/ngan-task-scope.md): ADMIN can read them and only switch the status.
 * Status changes and scheduling into a room serialize on the room row lock.
 */
@Service
public class RoomService {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private final RoomRepository rooms;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatService seats;
    private final Clock clock;

    public RoomService(RoomRepository rooms, ShowtimeRepository showtimes, ShowtimeSeatService seats, Clock clock) {
        this.rooms = rooms;
        this.showtimes = showtimes;
        this.seats = seats;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new CinemaException(BAD_REQUEST, "VALIDATION_ERROR", "page phải >= 0, size từ 1 đến 100.");
        }
        Page<Room> found = rooms.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new PageResponse<>(found.map(this::toResponse).getContent(), page, size,
            found.getTotalElements(), found.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RoomResponse detail(String roomId) {
        return toResponse(find(roomId));
    }

    @Transactional(readOnly = true)
    public RoomTemplateResponse template(String roomId) {
        Room room = find(roomId);
        List<TemplateSeat> layout = seats.physicalSeats(room.getTemplate().getId());
        return new RoomTemplateResponse(String.valueOf(room.getId()), room.getTemplate().getId(), room.getTemplate().getName(),
            room.getTemplate().getDescription(), layout.size(),
            layout.stream().map(seat -> new RoomTemplateResponse.Seat(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getSeatType() == null ? null : seat.getSeatType().name())).toList());
    }

    /**
     * Sending the current status again returns 200 without a new audit row.
     * When switching from ACTIVE to INACTIVE, the room must not be active in the next 1 month
     * (no running or upcoming showtimes within [now, now + 1 month]).
     */
    @Transactional
    public RoomResponse changeStatus(String roomId, String newStatus, String actorId, String traceId) {
        Integer id = parse(roomId);
        String current = rooms.lockStatus(id).orElseThrow(RoomService::notFound);
        if (!current.equals(newStatus)) {
            if (ACTIVE.equals(current) && INACTIVE.equals(newStatus)) {
                List<Showtime> occupying = occupyingShowtimes(id);
                if (!occupying.isEmpty()) {
                    throw new CinemaException(CONFLICT, "ROOM_HAS_UPCOMING_SHOWTIMES",
                        "Phòng còn " + occupying.size() + " suất chiếu đang diễn ra hoặc trong 1 tháng tới: "
                            + String.join(", ", occupying.stream().map(Showtime::getShowtimeId).limit(10).toList()));
                }
            }
            rooms.updateStatus(id, newStatus);
            rooms.insertAudit(id, current, newStatus, actorId, traceId);
        }
        return toResponse(rooms.findById(id).orElseThrow(RoomService::notFound));
    }

    /** For scheduling: locks the room and refuses an INACTIVE one. */
    @Transactional
    public Room lockActiveRoom(String roomId) {
        Integer id = parseForScheduling(roomId);
        String status = rooms.lockStatus(id)
            .orElseThrow(() -> new IllegalArgumentException("Phòng chiếu không tồn tại: " + roomId));
        if (!ACTIVE.equals(status)) {
            throw new CinemaException(CONFLICT, "ROOM_INACTIVE", "Phòng " + roomId + " đang ngừng hoạt động.");
        }
        return rooms.findById(id).orElseThrow();
    }

    /** Locks several rooms in a stable order, so two requests moving showtimes between rooms cannot deadlock. */
    @Transactional
    public void lockRooms(Collection<Integer> roomIds) {
        new TreeSet<>(roomIds).forEach(rooms::lockStatus);
    }

    public static Integer parseForScheduling(String roomId) {
        try {
            return Integer.valueOf(roomId.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Phòng chiếu không tồn tại: " + roomId);
        }
    }

    /**
     * Finds showtimes that occupy the room within the next 1 month ([now, now + 1 month]).
     * effectiveEndAt = show date + end time, plus one day when the end time is on the next day; the end time already
     * includes the cleaning buffer. Only showtimes currently running or starting within 1 month block deactivation.
     */
    private List<Showtime> occupyingShowtimes(Integer roomId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime oneMonthAhead = now.plusMonths(1);
        LocalDate fromDate = LocalDate.now(clock).minusDays(1);
        LocalDate toDate = oneMonthAhead.toLocalDate().plusDays(1);
        return showtimes.findByRoom_IdAndShowDateBetween(roomId, fromDate, toDate).stream()
            .filter(showtime -> effectiveEnd(showtime).isAfter(now) && effectiveStart(showtime).isBefore(oneMonthAhead))
            .toList();
    }

    static LocalDateTime effectiveStart(Showtime showtime) {
        return LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
    }

    static LocalDateTime effectiveEnd(Showtime showtime) {
        LocalDateTime end = LocalDateTime.of(showtime.getShowDate(), showtime.getEndTime());
        return showtime.getEndTime().isAfter(showtime.getStartTime()) ? end : end.plusDays(1);
    }

    private Room find(String roomId) {
        return rooms.findById(parse(roomId)).orElseThrow(RoomService::notFound);
    }

    private static Integer parse(String roomId) {
        try {
            return Integer.valueOf(roomId);
        } catch (NumberFormatException e) {
            throw notFound();
        }
    }

    private RoomResponse toResponse(Room room) {
        List<TemplateSeat> layout = seats.physicalSeats(room.getTemplate().getId());
        int vip = (int) layout.stream().filter(seat -> seat.getSeatType() == SeatType.VIP).count();
        return new RoomResponse(String.valueOf(room.getId()), room.getTemplate().getId(), room.getTemplate().getName(),
            room.getStatus(), layout.size(), layout.size() - vip, vip);
    }

    private static CinemaException notFound() {
        return new CinemaException(NOT_FOUND, "ROOM_NOT_FOUND", "Không tìm thấy phòng chiếu.");
    }
}
