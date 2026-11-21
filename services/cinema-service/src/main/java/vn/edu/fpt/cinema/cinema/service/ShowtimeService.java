package vn.edu.fpt.cinema.cinema.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.cinema.client.BookingClient;
import vn.edu.fpt.cinema.cinema.client.CatalogMovieClient;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeRequestDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeResponseDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeSeatMapResponse;
import vn.edu.fpt.cinema.cinema.entities.Room;
import vn.edu.fpt.cinema.cinema.entities.Showtime;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;
import vn.edu.fpt.cinema.cinema.enums.SeatType;
import vn.edu.fpt.cinema.cinema.exception.CinemaException;
import vn.edu.fpt.cinema.cinema.exception.ShowtimeExpiredException;
import vn.edu.fpt.cinema.cinema.exception.ShowtimeNotFoundException;
import vn.edu.fpt.cinema.cinema.repositories.RoomRepository;
import vn.edu.fpt.cinema.cinema.repositories.ShowtimeRepository;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeSeatService showtimeSeatService;
    private final CatalogMovieClient catalogMovieClient;
    private final RoomService roomService;
    private final BookingClient bookingClient;

    public ShowtimeService(
            ShowtimeRepository showtimeRepository,
            RoomRepository roomRepository,
            ShowtimeSeatService showtimeSeatService,
            CatalogMovieClient catalogMovieClient,
            RoomService roomService,
            BookingClient bookingClient
    ) {
        this.showtimeRepository = showtimeRepository;
        this.roomRepository = roomRepository;
        this.showtimeSeatService = showtimeSeatService;
        this.catalogMovieClient = catalogMovieClient;
        this.roomService = roomService;
        this.bookingClient = bookingClient;
    }

    // ==========================================
    // 1. MANAGE SHOWTIMES (ADMIN - Phan Đức Thọ)
    // ==========================================

    /**
     * View Showtimes For Admin
     */
    public List<ShowtimeResponseDTO> getAllShowtimes() {
        return showtimeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Create Showtime (Admin)
     * Chặn không cho tạo suất chiếu có thời gian bắt đầu trong quá khứ
     */
    @Transactional
    public ShowtimeResponseDTO createShowtime(ShowtimeRequestDTO dto) {
        if (dto.showDate() == null || dto.startTime() == null) {
            throw new IllegalArgumentException("Ngày chiếu và giờ chiếu không được để trống.");
        }

        LocalDateTime showStart = LocalDateTime.of(dto.showDate(), dto.startTime());
        if (showStart.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu với thời gian trong quá khứ.");
        }

        int duration = getMovieDuration(dto.movieId());
        String movieTitle = getMovieTitle(dto.movieId());
        LocalDateTime candidateEnd = showStart.plusMinutes(duration + 15); // +15 phút đệm dọn phòng
        LocalTime endTime = candidateEnd.toLocalTime();

        // Catalog is read before taking the room lock, so no SQL lock waits on HTTP (AGENTS 4).
        // The lock serializes this with room status changes and other scheduling in the same room.
        Room room = roomService.lockActiveRoom(dto.roomId());
        List<Showtime> conflicts = findConflictingShowtimes(room.getId(), showStart, candidateEnd, null);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Khung giờ bị trùng với suất chiếu khác trong phòng " + dto.roomId());
        }

        Showtime showtime = new Showtime();
        // New IDs are lowercase UUIDs (AGENTS 6); legacy IDs such as SW00023 stay as they are.
        showtime.setShowtimeId(java.util.UUID.randomUUID().toString());
        showtime.setMovieId(dto.movieId());
        showtime.setRoom(room);
        showtime.setShowDate(dto.showDate());
        showtime.setStartTime(dto.startTime());
        showtime.setEndTime(endTime);
        showtime.setSalesStatus("DRAFT"); // Mặc định là DRAFT, cần Open Sales để mở bán

        Showtime saved = showtimeRepository.save(showtime);
        return ShowtimeResponseDTO.fromEntity(saved, movieTitle);
    }

    /**
     * Update Showtime (Admin)
     */
    @Transactional
    public ShowtimeResponseDTO updateShowtime(String id, ShowtimeRequestDTO dto) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + id);
        }

        if ("OPEN".equalsIgnoreCase(showtime.getSalesStatus())) {
            throw new IllegalStateException("Không thể sửa suất chiếu đang mở bán. Vui lòng đóng bán trước.");
        }

        LocalDateTime showStart = LocalDateTime.of(dto.showDate(), dto.startTime());
        if (showStart.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu suất chiếu không thể ở trong quá khứ.");
        }

        int duration = getMovieDuration(dto.movieId());
        String movieTitle = getMovieTitle(dto.movieId());
        LocalDateTime candidateEnd = showStart.plusMinutes(duration + 15);
        LocalTime endTime = candidateEnd.toLocalTime();

        // Lock the old and new rooms in a stable order, then require the target room to be ACTIVE.
        Integer targetRoomId = RoomService.parseForScheduling(dto.roomId());
        if (showtime.getRoom() != null) {
            roomService.lockRooms(java.util.List.of(showtime.getRoom().getId(), targetRoomId));
        }
        Room room = roomService.lockActiveRoom(dto.roomId());
        List<Showtime> conflicts = findConflictingShowtimes(room.getId(), showStart, candidateEnd, id);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Khung giờ cập nhật bị trùng với suất chiếu khác trong phòng " + dto.roomId());
        }

        showtime.setMovieId(dto.movieId());
        showtime.setRoom(room);
        showtime.setShowDate(dto.showDate());
        showtime.setStartTime(dto.startTime());
        showtime.setEndTime(endTime);

        Showtime saved = showtimeRepository.save(showtime);
        return ShowtimeResponseDTO.fromEntity(saved, movieTitle);
    }

    /**
     * Delete Showtime (Admin)
     */
    @Transactional
    public boolean deleteShowtime(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) return false;
        showtimeRepository.delete(showtime);
        return true;
    }

    /**
     * Open Showtime Sales (sync seat inventory) (Admin - Phan Đức Thọ)
     * Chặn không cho mở bán suất chiếu trong quá khứ
     */
    @Transactional
    public ShowtimeResponseDTO openShowtimeSales(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + id);
        }

        // CHẶN: Kiểm tra không mở bán suất chiếu trong quá khứ
        LocalDateTime showStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
        if (showStart.isBefore(LocalDateTime.now())) {
            throw new ShowtimeExpiredException("Không thể mở bán cho suất chiếu đã qua thời gian bắt đầu.");
        }

        // Selling in an INACTIVE room is not allowed; the room lock also orders this after a status change.
        if (showtime.getRoom() != null) {
            roomService.lockActiveRoom(String.valueOf(showtime.getRoom().getId()));
        }

        // Sync seat inventory: tạo ghế nếu chưa có
        if (showtime.getRoom() != null && showtime.getRoom().getTemplate() != null) {
            if (!showtimeSeatService.hasSeatsForShowtime(id)) {
                showtimeSeatService.createShowtimeSeats(id, showtime.getRoom().getTemplate().getId());
            }
        }

        // Booking decides first (AGENTS 4): only after it stored the snapshot and seats is the showtime shown as OPEN.
        // A failure throws and rolls this transaction back; a retry is safe because booking's init is idempotent.
        // shortcut: the room lock is held during this HTTP call (bounded by 2 s/3 s timeouts); move to a persisted
        // Sales_Workflow with retries if open-sales ever needs to survive booking outages.
        bookingClient.initSales(salesSnapshot(showtime));

        showtime.setSalesStatus("OPEN");
        return toResponseDTO(showtimeRepository.save(showtime));
    }

    /**
     * Close Showtime Sales (Admin - Phan Đức Thọ)
     */
    @Transactional
    public ShowtimeResponseDTO closeShowtimeSales(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + id);
        }

        // Booking closes first (AGENTS 4): it stops new holds and bookings. If that fails, cinema keeps showing the
        // showtime as it was instead of claiming a close that did not happen.
        bookingClient.closeSales(id);

        showtime.setSalesStatus("CLOSED");
        return toResponseDTO(showtimeRepository.save(showtime));
    }

    private BookingClient.InitSalesRequest salesSnapshot(Showtime showtime) {
        if (showtime.getRoom() == null || showtime.getRoom().getTemplate() == null) {
            throw new IllegalStateException("Suất chiếu chưa có phòng hoặc sơ đồ ghế, không thể mở bán.");
        }
        List<BookingClient.InitSalesRequest.SeatItem> seatItems = showtimeSeatService
            .physicalSeats(showtime.getRoom().getTemplate().getId()).stream()
            .map(s -> new BookingClient.InitSalesRequest.SeatItem(
                s.getId(),
                s.getRowLabel(),
                s.getSeatNumber(),
                s.getSeatType() != null ? s.getSeatType().name() : "STANDARD",
                s.getSeatType() == SeatType.VIP ? new java.math.BigDecimal("100000.00") : new java.math.BigDecimal("80000.00")
            ))
            .toList();
        return new BookingClient.InitSalesRequest(
            showtime.getShowtimeId(),
            showtime.getMovieId(),
            snapshotMovieTitle(showtime.getMovieId()),
            showtime.getRoom().getId(),
            "Room " + showtime.getRoom().getId(), // same room naming as existing booking snapshots
            showtime.getShowDate(),
            showtime.getStartTime(),
            showtime.getEndTime(),
            seatItems
        );
    }

    /** The title is printed on every ticket, so a placeholder is not acceptable here (AGENTS 8). */
    private String snapshotMovieTitle(String movieId) {
        // CatalogMovieClient answers null for every failure, so "catalog down" and "no such movie" look the same.
        CatalogMovieClient.MovieMetadataDTO movie = catalogMovieClient.getMovieById(movieId);
        if (movie == null || movie.title() == null || movie.title().isBlank()) {
            throw new CinemaException(HttpStatus.SERVICE_UNAVAILABLE, "MOVIE_TITLE_UNAVAILABLE",
                "Không lấy được tên phim " + movieId + " từ catalog-service (catalog không phản hồi hoặc không có phim này), chưa thể mở bán.");
        }
        return movie.title();
    }

    // ==========================================
    // 2. SHOWTIMES & SEATS (CUSTOMER - Phan Đức Thọ)
    // ==========================================

    /**
     * View Showtimes (Customer)
     * Chỉ hiển thị các suất chiếu:
     * 1. Đang mở bán (salesStatus = "OPEN")
     * 2. CHẶN: Chưa qua thời gian bắt đầu (startTime > LocalDateTime.now())
     */
    public List<ShowtimeResponseDTO> getCustomerShowtimes(String movieId, LocalDate date) {
        List<Showtime> list;
        if (movieId != null && !movieId.isBlank() && date != null) {
            list = showtimeRepository.findByMovieIdAndShowDateOrderByStartTimeAsc(movieId, date);
        } else if (movieId != null && !movieId.isBlank()) {
            list = showtimeRepository.findByMovieId(movieId);
        } else if (date != null) {
            list = showtimeRepository.findByShowDate(date);
        } else {
            list = showtimeRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();

        return list.stream()
                .filter(s -> "OPEN".equalsIgnoreCase(s.getSalesStatus()))
                .filter(s -> {
                    if (s.getShowDate() == null || s.getStartTime() == null) return false;
                    LocalDateTime start = LocalDateTime.of(s.getShowDate(), s.getStartTime());
                    return start.isAfter(now); // CHẶN: Chỉ lấy suất chiếu chưa bắt đầu
                })
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Select Showtime (Customer)
     * Khi khách hàng bấm chọn suất chiếu để chuẩn bị đặt vé:
     * - CHẶN: Nếu suất chiếu đã qua thời gian bắt đầu -> Ném ShowtimeExpiredException
     * - CHẶN: Nếu suất chiếu chưa mở bán hoặc đã đóng -> Ném IllegalStateException
     */
    public ShowtimeResponseDTO getCustomerSelectedShowtime(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) {
            throw new IllegalArgumentException("Suất chiếu không tồn tại: " + id);
        }

        // CHẶN 1: Kiểm tra suất chiếu đã qua thời gian bắt đầu
        LocalDateTime showStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
        if (showStart.isBefore(LocalDateTime.now())) {
            throw new ShowtimeExpiredException("Không thể đặt vé cho suất chiếu đã qua.");
        }

        // CHẶN 2: Kiểm tra trạng thái bán vé
        if (!"OPEN".equalsIgnoreCase(showtime.getSalesStatus())) {
            throw new IllegalStateException("Suất chiếu hiện chưa mở bán hoặc đã đóng bán.");
        }

        return toResponseDTO(showtime);
    }

    /** Fixed layout of the showtime's room; it carries no availability. */
    public ShowtimeSeatMapResponse getSeatMap(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) {
            throw new ShowtimeNotFoundException("Không tìm thấy suất chiếu: " + id);
        }
        Room room = showtime.getRoom();
        String templateId = room == null || room.getTemplate() == null ? null : room.getTemplate().getId();
        List<TemplateSeat> seats = templateId == null ? List.of() : showtimeSeatService.physicalSeats(templateId);
        return ShowtimeSeatMapResponse.of(showtime, getMovieTitle(showtime.getMovieId()), templateId, seats);
    }

    public ShowtimeResponseDTO getShowtimeById(String id) {
        Showtime showtime = showtimeRepository.findByShowtimeId(id);
        if (showtime == null) return null;
        return toResponseDTO(showtime);
    }

    public List<ShowtimeResponseDTO> getShowtimesByMovie(String movieId) {
        return showtimeRepository.findByMovieId(movieId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ShowtimeResponseDTO> getShowtimesByDate(LocalDate date) {
        return showtimeRepository.findByShowDate(date).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ShowtimeResponseDTO> getShowtimesByMovieAndDate(String movieId, LocalDate date) {
        return showtimeRepository.findByMovieIdAndShowDateOrderByStartTimeAsc(movieId, date).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private int getMovieDuration(String movieId) {
        try {
            CatalogMovieClient.MovieMetadataDTO movie = catalogMovieClient.getMovieById(movieId);
            if (movie != null && movie.duration() > 0) return movie.duration();
        } catch (Exception ignored) {}
        return 120; // default 120 mins
    }

    private String getMovieTitle(String movieId) {
        try {
            CatalogMovieClient.MovieMetadataDTO movie = catalogMovieClient.getMovieById(movieId);
            if (movie != null && movie.title() != null) return movie.title();
        } catch (Exception ignored) {}
        return "Movie " + movieId;
    }

    private ShowtimeResponseDTO toResponseDTO(Showtime showtime) {
        String title = getMovieTitle(showtime.getMovieId());
        return ShowtimeResponseDTO.fromEntity(showtime, title);
    }

    public static LocalDateTime getEffectiveStart(Showtime showtime) {
        return LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
    }

    public static LocalDateTime getEffectiveEnd(Showtime showtime) {
        LocalDateTime end = LocalDateTime.of(showtime.getShowDate(), showtime.getEndTime());
        return showtime.getEndTime().isAfter(showtime.getStartTime()) ? end : end.plusDays(1);
    }

    private static boolean overlaps(LocalDateTime startA, LocalDateTime endA, LocalDateTime startB, LocalDateTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private List<Showtime> findConflictingShowtimes(Integer roomId, LocalDateTime candidateStart, LocalDateTime candidateEnd, String excludeShowtimeId) {
        LocalDate searchStart = candidateStart.toLocalDate().minusDays(1);
        LocalDate searchEnd = candidateEnd.toLocalDate().plusDays(1);
        List<Showtime> nearby = showtimeRepository.findByRoom_IdAndShowDateBetween(roomId, searchStart, searchEnd);
        return nearby.stream()
                .filter(s -> excludeShowtimeId == null || !s.getShowtimeId().equals(excludeShowtimeId))
                .filter(s -> {
                    LocalDateTime sStart = getEffectiveStart(s);
                    LocalDateTime sEnd = getEffectiveEnd(s);
                    return overlaps(candidateStart, candidateEnd, sStart, sEnd);
                })
                .toList();
    }
}
