package vn.edu.fpt.cinema.cinema.entities;

import jakarta.persistence.*;
import vn.edu.fpt.cinema.cinema.converters.SeatTypeConverter;
import vn.edu.fpt.cinema.cinema.enums.SeatType;

@Entity
@Table(name = "Template_Seat")
public class TemplateSeat {

    @Id
    @Column(name = "template_seat_id")
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private Template template;

    @Column(name = "row_label")
    private String rowLabel;

    @Column(name = "seat_number")
    private int seatNumber;

    @Convert(converter = SeatTypeConverter.class)
    @Column(name = "seat_type")
    private SeatType seatType;

    public TemplateSeat() {}

    public TemplateSeat(String id, Template template, String rowLabel, int seatNumber, SeatType seatType) {
        this.id = id;
        this.template = template;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
}

