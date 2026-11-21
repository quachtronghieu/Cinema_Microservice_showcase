package vn.edu.fpt.cinema.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.edu.fpt.cinema.cinema.client.CatalogMovieClient;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeRequestDTO;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeResponseDTO;
import vn.edu.fpt.cinema.cinema.entities.Room;
import vn.edu.fpt.cinema.cinema.entities.Template;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;
import vn.edu.fpt.cinema.cinema.dto.ShowtimeSeatMapResponse;
import vn.edu.fpt.cinema.cinema.enums.SeatType;
import vn.edu.fpt.cinema.cinema.repositories.RoomRepository;
import vn.edu.fpt.cinema.cinema.repositories.TemplateRepository;
import vn.edu.fpt.cinema.cinema.repositories.TemplateSeatRepository;

@SpringBootTest(properties = {"eureka.client.enabled=false",
    "cinema.security.jwk-set-uri=https://issuer.test/jwks"})
@Testcontainers(disabledWithoutDocker = true)
class ShowtimeDatabaseIT {
    static final String UUID_MOVIE = "8ef527b5-72f7-4765-a032-540a4a3023b6";

    @Container static final MSSQLServerContainer<?> SQL =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04").acceptLicense();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL::getJdbcUrl);
        registry.add("spring.datasource.username", SQL::getUsername);
        registry.add("spring.datasource.password", SQL::getPassword);
        registry.add("spring.flyway.url", SQL::getJdbcUrl);
        registry.add("spring.flyway.user", SQL::getUsername);
        registry.add("spring.flyway.password", SQL::getPassword);
    }

    @MockitoBean CatalogMovieClient catalog;
    @MockitoBean vn.edu.fpt.cinema.cinema.client.BookingClient bookingClient;
    @Autowired ShowtimeService showtimes;
    @Autowired ShowtimeSeatService seats;
    @Autowired TemplateRepository templates;
    @Autowired TemplateSeatRepository templateSeats;
    @Autowired RoomRepository rooms;
    @Autowired JdbcTemplate jdbc;
    @Autowired vn.edu.fpt.cinema.cinema.repositories.ShowtimeRepository showtimeRepository;

    static String roomId;

    @BeforeEach
    void room() {
        // Open Sales needs the real movie title (503 MOVIE_TITLE_UNAVAILABLE otherwise); the mock is reset after each test.
        when(catalog.getMovieById(UUID_MOVIE)).thenReturn(new CatalogMovieClient.MovieMetadataDTO(UUID_MOVIE, "Test movie", 120, "Drama", "img.jpg", "T13"));
        if (roomId != null) return;
        Template template = templates.save(new Template("TP000001", "Standard", "Test layout"));
        templateSeats.save(new TemplateSeat("TS000001", template, "A", 1, SeatType.STANDARD));
        templateSeats.save(new TemplateSeat("TS000002", template, "A", 2, SeatType.STANDARD));
        roomId = String.valueOf(rooms.save(new Room(template, "ACTIVE")).getId());
    }

    @Test
    void createsShowtimeForUuidMovieWithSeatsAndRejectsOverlap() {
        LocalDate day = LocalDate.now().plusDays(1);

        ShowtimeResponseDTO created = showtimes.createShowtime(request(day, "19:30:00"));

        assertThat(created.movieId()).isEqualTo(UUID_MOVIE);
        showtimes.openShowtimeSales(created.showtimeId());
        assertThat(seats.getSeatsEntityByShowtimeId(created.showtimeId())).hasSize(2);
        var map = showtimes.getSeatMap(created.showtimeId());
        assertThat(map.templateId()).isEqualTo("TP000001");
        assertThat(map.roomId()).isEqualTo(roomId);
        assertThat(map.seats()).extracting(ShowtimeSeatMapResponse.Seat::templateSeatId).containsExactly("TS000001", "TS000002");
        assertThatThrownBy(() -> showtimes.createShowtime(request(day, "20:00:00")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(showtimes.getShowtimesByMovieAndDate(UUID_MOVIE, day)).hasSize(1);
    }

    @Test
    void endOfOneShowtimeMayTouchTheStartOfTheNext() {
        LocalDate day = LocalDate.now().plusDays(2);

        showtimes.createShowtime(request(day, "10:00:00"));
        showtimes.createShowtime(request(day, "12:15:00"));

        assertThat(showtimes.getShowtimesByMovieAndDate(UUID_MOVIE, day)).hasSize(2);
    }

    @Test
    void midnightCrossingShowtimeRejectsOverlapAcrossDays() {
        LocalDate day1 = LocalDate.now().plusDays(5);
        LocalDate day2 = day1.plusDays(1);

        // Day 1 at 23:00 -> duration 120 + 15 buffer = 135 mins -> ends next day at 01:15
        ShowtimeResponseDTO nightShow = showtimes.createShowtime(request(day1, "23:00:00"));
        assertThat(nightShow.endTime()).isEqualTo(LocalTime.of(1, 15));

        // Overlap 1: Day 2 at 00:30 (starts before nightShow ends at 01:15) -> must reject
        assertThatThrownBy(() -> showtimes.createShowtime(request(day2, "00:30:00")))
            .isInstanceOf(IllegalArgumentException.class);

        // Overlap 2: Day 1 at 23:30 (starts before nightShow ends) -> must reject
        assertThatThrownBy(() -> showtimes.createShowtime(request(day1, "23:30:00")))
            .isInstanceOf(IllegalArgumentException.class);

        // Boundary touch: Day 2 at 01:15 (exact end time of nightShow) -> allowed
        showtimes.createShowtime(request(day2, "01:15:00"));

        // Day 1 at 20:45 (ends at 23:00, touching start of nightShow) -> allowed
        showtimes.createShowtime(request(day1, "20:45:00"));
    }

    @Test
    void listsOnlyTheRequestedDaySortedByStartTime() {
        LocalDate day = LocalDate.now().plusDays(3);
        showtimes.createShowtime(request(day, "19:30:00"));
        showtimes.createShowtime(request(day, "08:00:00"));
        showtimes.createShowtime(request(day.plusDays(1), "19:30:00"));

        List<ShowtimeResponseDTO> found = showtimes.getShowtimesByMovieAndDate(UUID_MOVIE, day);

        assertThat(found).extracting(ShowtimeResponseDTO::showDate).containsOnly(day);
        assertThat(found).extracting(ShowtimeResponseDTO::startTime)
            .containsExactly(LocalTime.of(8, 0), LocalTime.of(19, 30));
    }

    @Test
    void newShowtimesAndSeatsGetUuids_andNeverOverwriteLegacyRows() {
        // SW00024 sorts after every ST... ID; the old generator then always returned ST000001 and overwrote it.
        LocalDate day = LocalDate.now().plusDays(10);
        jdbc.update("INSERT INTO dbo.Showtime (showtime_id, movie_id, room_id, show_date, start_time, end_time, sales_status) "
            + "VALUES ('SW00024', 'MV000016', ?, ?, '08:00', '09:00', 'OPEN')", Integer.valueOf(roomId), day.minusDays(1));
        jdbc.update("INSERT INTO dbo.Showtime_Seat (showtime_seat_id, showtime_id, template_seat_id, status) VALUES ('HS200155', 'SW00024', 'TS000001', 'AVAILABLE')");

        ShowtimeResponseDTO first = showtimes.createShowtime(request(day, "10:00:00"));
        ShowtimeResponseDTO second = showtimes.createShowtime(request(day, "14:00:00"));
        showtimes.openShowtimeSales(second.showtimeId());

        String uuid = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        assertThat(first.showtimeId()).matches(uuid).isNotEqualTo(second.showtimeId());
        assertThat(second.showtimeId()).matches(uuid);
        assertThat(jdbc.queryForObject("SELECT start_time FROM dbo.Showtime WHERE showtime_id = 'SW00024'", String.class))
            .startsWith("08:00");
        assertThat(seats.getSeatsEntityByShowtimeId(second.showtimeId()))
            .extracting(vn.edu.fpt.cinema.cinema.entities.ShowtimeSeat::getShowtimeSeatID).allMatch(id -> id.matches(uuid));
        assertThat(jdbc.queryForObject("SELECT showtime_id FROM dbo.Showtime_Seat WHERE showtime_seat_id = 'HS200155'", String.class))
            .isEqualTo("SW00024");
    }

    @Test
    void savingANewShowtimeWithAnExistingIdFailsInsteadOfOverwriting() {
        LocalDate day = LocalDate.now().plusDays(12);
        ShowtimeResponseDTO existing = showtimes.createShowtime(request(day, "10:00:00"));
        var copy = new vn.edu.fpt.cinema.cinema.entities.Showtime();
        copy.setShowtimeId(existing.showtimeId());
        copy.setMovieId("OTHER");
        copy.setRoom(rooms.findById(Integer.valueOf(roomId)).orElseThrow());
        copy.setShowDate(day.plusDays(1));
        copy.setStartTime(LocalTime.of(20, 0));
        copy.setEndTime(LocalTime.of(22, 0));

        assertThatThrownBy(() -> showtimeRepository.saveAndFlush(copy))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT movie_id FROM dbo.Showtime WHERE showtime_id = ?", String.class, existing.showtimeId()))
            .isEqualTo(UUID_MOVIE);
    }

    private static ShowtimeRequestDTO request(LocalDate day, String startTime) {
        return new ShowtimeRequestDTO(UUID_MOVIE, roomId, day, LocalTime.parse(startTime));
    }
}
