package vn.edu.fpt.cinema.cinema.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Template")
public class Template {

    @Id
    @Column(name = "template_id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TemplateSeat> templateSeats = new ArrayList<>();

    public Template() {}

    public Template(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<TemplateSeat> getTemplateSeats() { return templateSeats; }
    public void setTemplateSeats(List<TemplateSeat> templateSeats) { this.templateSeats = templateSeats; }
}

