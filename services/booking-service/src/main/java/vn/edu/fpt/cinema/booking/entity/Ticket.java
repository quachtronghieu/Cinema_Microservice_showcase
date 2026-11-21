package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** One ticket per paid booking (unique booking_id), covering all of its seats. */
@Entity
@Table(name = "Ticket")
public class Ticket {
    @Id @Column(name = "ticket_id") private String ticketId;
    @Column(name = "booking_id", nullable = false) private String bookingId;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "checked_in", nullable = false) private boolean checkedIn;
    @Column(name = "checked_in_at") private Instant checkedInAt;
    @Column(name = "checked_in_by") private String checkedInBy;

    protected Ticket() {}

    public Ticket(String bookingId, Instant issuedAt) {
        this.ticketId = UUID.randomUUID().toString();
        this.bookingId = bookingId;
        this.issuedAt = issuedAt;
    }

    public String getTicketId() { return ticketId; }
    public String getBookingId() { return bookingId; }
    public Instant getIssuedAt() { return issuedAt; }
    public boolean isCheckedIn() { return checkedIn; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public String getCheckedInBy() { return checkedInBy; }
}
