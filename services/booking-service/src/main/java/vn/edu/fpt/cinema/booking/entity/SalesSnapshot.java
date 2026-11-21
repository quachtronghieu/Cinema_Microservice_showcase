package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Booking's copy of a showtime that is open or closed for sales (owned here, published by cinema). */
@Entity
@Table(name = "Sales_Snapshot")
public class SalesSnapshot {
    @Id @Column(name = "showtime_id") private String showtimeId;
    @Column(name = "snapshot_version", nullable = false) private Long snapshotVersion;
    @Column(name = "sales_status", nullable = false) private String salesStatus;
    @Column(name = "movie_id", nullable = false) private String movieId;
    @Column(name = "movie_title") private String movieTitle;
    @Column(name = "room_id") private Integer roomId;
    @Column(name = "room_name") private String roomName;
    @Column(name = "show_date") private LocalDate showDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;

    protected SalesSnapshot() {}

    public SalesSnapshot(String showtimeId, String salesStatus) {
        this.showtimeId = showtimeId;
        this.salesStatus = salesStatus;
        this.snapshotVersion = 1L;
    }

    public SalesSnapshot(String showtimeId, Long snapshotVersion, String salesStatus, String movieId,
                         String movieTitle, Integer roomId, String roomName,
                         LocalDate showDate, LocalTime startTime, LocalTime endTime) {
        this.showtimeId = showtimeId;
        this.snapshotVersion = snapshotVersion != null ? snapshotVersion : 1L;
        this.salesStatus = salesStatus;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.roomId = roomId;
        this.roomName = roomName;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getShowtimeId() { return showtimeId; }
    public Long getSnapshotVersion() { return snapshotVersion; }
    public String getSalesStatus() { return salesStatus; }
    public boolean isOpen() { return "OPEN".equals(salesStatus); }
    public String getMovieId() { return movieId; }
    public String getMovieTitle() { return movieTitle; }
    public Integer getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public LocalDate getShowDate() { return showDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
