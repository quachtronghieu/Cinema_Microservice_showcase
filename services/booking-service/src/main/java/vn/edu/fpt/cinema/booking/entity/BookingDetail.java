package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** One purchased line with its name and price frozen at booking time (total_price is computed by SQL Server). */
@Entity
@Table(name = "Booking_Detail")
public class BookingDetail {
    @Id @Column(name = "booking_detail_id") private String bookingDetailId;
    @Column(name = "booking_id", nullable = false) private String bookingId;
    @Column(name = "item_type", nullable = false) private String itemType;
    @Column(name = "showtime_seat_id") private String showtimeSeatId;
    @Column(name = "concession_id") private String concessionId;
    @Column(name = "item_name", nullable = false) private String itemName;
    @Column(name = "seat_type") private String seatType;
    @Column(name = "quantity", nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    /** Computed and persisted by SQL Server: quantity * unit_price. */
    @Column(name = "total_price", insertable = false, updatable = false, precision = 19, scale = 2) private BigDecimal totalPrice;

    protected BookingDetail() {}

    private BookingDetail(String bookingId, String itemType, String showtimeSeatId, String concessionId,
                          String itemName, String seatType, int quantity, BigDecimal unitPrice) {
        this.bookingDetailId = UUID.randomUUID().toString();
        this.bookingId = bookingId;
        this.itemType = itemType;
        this.showtimeSeatId = showtimeSeatId;
        this.concessionId = concessionId;
        this.itemName = itemName;
        this.seatType = seatType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getBookingId() { return bookingId; }
    public String getItemType() { return itemType; }
    public String getConcessionId() { return concessionId; }
    public String getItemName() { return itemName; }
    public String getSeatType() { return seatType; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }

    public static BookingDetail seat(String bookingId, ShowtimeSeat seat) {
        return new BookingDetail(bookingId, "SEAT", seat.getShowtimeSeatId(), null, seat.label(), seat.getSeatType(), 1, seat.getUnitPrice());
    }

    public static BookingDetail concession(String bookingId, String concessionId, String name, int quantity, BigDecimal unitPrice) {
        return new BookingDetail(bookingId, "CONCESSION", null, concessionId, name, null, quantity, unitPrice);
    }
}
