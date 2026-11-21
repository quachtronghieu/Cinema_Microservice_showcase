package vn.edu.fpt.cinema.cinema.entities;

import jakarta.persistence.*;
import vn.edu.fpt.cinema.cinema.converters.SeatStatusConverter;
import org.springframework.data.domain.Persistable;
import vn.edu.fpt.cinema.cinema.enums.SeatStatus;

@Entity
@Table(name = "Showtime_Seat")
public class ShowtimeSeat implements Persistable<String> {

    @Id
    @Column(name = "showtime_seat_id", length = 36)
    private String showtimeSeatID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "showtime_id", referencedColumnName = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_seat_id", referencedColumnName = "template_seat_id", nullable = false)
    private TemplateSeat templateSeat;

    @Convert(converter = SeatStatusConverter.class)
    @Column(name = "status")
    private SeatStatus status;

    public ShowtimeSeat() {}

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
    public String getId() { return showtimeSeatID; }

    public ShowtimeSeat(String showtimeSeatID, Showtime showtime, TemplateSeat templateSeat, SeatStatus status) {
        this.showtimeSeatID = showtimeSeatID;
        this.showtime = showtime;
        this.templateSeat = templateSeat;
        this.status = status;
    }

    public String getShowtimeSeatID() { return showtimeSeatID; }
    public void setShowtimeSeatID(String showtimeSeatID) { this.showtimeSeatID = showtimeSeatID; }

    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }

    public TemplateSeat getTemplateSeat() { return templateSeat; }
    public void setTemplateSeat(TemplateSeat templateSeat) { this.templateSeat = templateSeat; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
}

