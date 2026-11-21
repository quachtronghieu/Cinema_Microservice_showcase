package vn.edu.fpt.cinema.catalog.concession.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Concession", schema = "dbo")
public class Concession {
    @Id @Column(name = "concession_id", length = 36, nullable = false)
    private String concessionId;
    @Enumerated(EnumType.STRING) @Column(name = "type", length = 2, nullable = false)
    private ConcessionType type;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 255) private String description;
    @Column(nullable = false, length = 255) private String img;
    @Column(nullable = false) private boolean active;
    @Version @Column(nullable = false) private Long version;

    protected Concession() {}
    public Concession(String id, ConcessionType type, String name, String description, BigDecimal price, String img) {
        this.concessionId = id; this.type = type; this.name = name;
        this.description = description; this.price = price; this.img = img; this.active = true;
    }
    public boolean matches(ConcessionType type, String name, String description, BigDecimal price, String img) {
        return this.type == type && this.name.equals(name) && this.description.equals(description)
                && this.price.compareTo(price) == 0 && this.img.equals(img);
    }
    public void update(ConcessionType type, String name, String description, BigDecimal price, String img) {
        this.type = type; this.name = name; this.description = description; this.price = price; this.img = img;
    }
    public void deactivate() { this.active = false; }
    public String getConcessionId() { return concessionId; }
    public ConcessionType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getImg() { return img; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
}
