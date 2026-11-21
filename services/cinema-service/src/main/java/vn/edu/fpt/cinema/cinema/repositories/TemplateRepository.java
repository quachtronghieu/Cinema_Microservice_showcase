package vn.edu.fpt.cinema.cinema.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.cinema.entities.Template;

@Repository
public interface TemplateRepository extends JpaRepository<Template, String> {
}

