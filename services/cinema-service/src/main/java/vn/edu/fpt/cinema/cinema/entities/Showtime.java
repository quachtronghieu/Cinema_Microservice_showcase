package vn.edu.fpt.cinema.cinema.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "Showtime")
public class Showtime implements Persistable<String> {

    @Id
    @Column(name = "showtime_id", length = 36)
    private String showtimeId;

    @Column(name = "movie_id", length = 36, nullable = false)
    private String movieId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", nullable = false)
    private Room room;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", columnDefinition = "time")
    private LocalTime startTime;

    @Column(name = "end_time", columnDefinition = "time")
    private LocalTime endTime;

    @Column(name = "sales_status", length = 20)
    private String salesStatus = "OPEN"; // DRAFT, OPEN, CLOSED

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

    public Showtime() {}

    /**
     * IDs are assigned before saving, so without this Spring Data would merge and silently overwrite an existing row
     * with the same ID. New entities are inserted instead, and a duplicate ID fails on the primary key.
     */
    @Transient
    private boolean newEntity = true;

    @PostLoad
    @PostPersist
    void markNotNew() { newEntity = false; }

    @Override
    public boolean isNew() { return newEntity; }

    @Override
    public String getId() { return showtimeId; }

    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getSalesStatus() { return salesStatus != null ? salesStatus : "OPEN"; }
    public void setSalesStatus(String salesStatus) { this.salesStatus = salesStatus; }

    public List<ShowtimeSeat> getShowtimeSeats() { return showtimeSeats; }
    public void setShowtimeSeats(List<ShowtimeSeat> showtimeSeats) { this.showtimeSeats = showtimeSeats; }

    @Transient
    public String getDisplayStatus() {
        if (showDate != null && showDate.isAfter(LocalDate.now())) {
            return "Early Screening";
        }
        return "Normal";
    }
}
