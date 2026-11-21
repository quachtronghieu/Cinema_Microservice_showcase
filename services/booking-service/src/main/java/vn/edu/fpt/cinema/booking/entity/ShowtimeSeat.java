package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Showtime_Seat")
public class ShowtimeSeat {
    @Id @Column(name = "showtime_seat_id") private String showtimeSeatId;
    @Column(name = "showtime_id", nullable = false) private String showtimeId;
    @Column(name = "template_seat_id") private String templateSeatId;
    @Column(name = "row_label", nullable = false) private String rowLabel;
    @Column(name = "seat_number", nullable = false) private int seatNumber;
    @Column(name = "seat_type", nullable = false) private String seatType;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "hold_id") private String holdId;

    protected ShowtimeSeat() {}

    public ShowtimeSeat(String showtimeSeatId, String showtimeId, String rowLabel, int seatNumber,
                        String seatType, BigDecimal unitPrice, String status, String holdId) {
        this(showtimeSeatId, showtimeId, null, rowLabel, seatNumber, seatType, unitPrice, status, holdId);
    }

    public ShowtimeSeat(String showtimeSeatId, String showtimeId, String templateSeatId, String rowLabel, int seatNumber,
                        String seatType, BigDecimal unitPrice, String status, String holdId) {
        this.showtimeSeatId = showtimeSeatId;
        this.showtimeId = showtimeId;
        this.templateSeatId = templateSeatId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.unitPrice = unitPrice;
        this.status = status;
        this.holdId = holdId;
    }

    public String getShowtimeSeatId() { return showtimeSeatId; }
    public String getShowtimeId() { return showtimeId; }
    public String getTemplateSeatId() { return templateSeatId; }
    public String getRowLabel() { return rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public String getSeatType() { return seatType; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getStatus() { return status; }
    public String getHoldId() { return holdId; }
    public String label() { return rowLabel + seatNumber; }
}
