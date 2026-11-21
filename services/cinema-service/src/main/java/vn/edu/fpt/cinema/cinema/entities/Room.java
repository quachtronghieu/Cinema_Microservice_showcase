package vn.edu.fpt.cinema.cinema.entities;

import jakarta.persistence.*;

/** Fixed room. room_id is INT IDENTITY (AGENTS 10, 20); several rooms may use one template. */
@Entity
@Table(name = "Room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "template_id", referencedColumnName = "template_id", nullable = false)
    private Template template;

    @Column(name = "status", nullable = false)
    private String status;

    public Room() {}

    public Room(Template template, String status) {
        this.template = template;
        this.status = status;
    }

    public Integer getId() { return id; }

    public Template getTemplate() { return template; }

    public String getStatus() { return status; }

    public boolean isActive() { return "ACTIVE".equals(status); }
}
