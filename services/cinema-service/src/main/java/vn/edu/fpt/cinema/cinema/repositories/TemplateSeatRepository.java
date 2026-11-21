package vn.edu.fpt.cinema.cinema.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;

@Repository
public interface TemplateSeatRepository extends JpaRepository<TemplateSeat, String> {
    List<TemplateSeat> findAllByTemplate_Id(String templateId);
}

