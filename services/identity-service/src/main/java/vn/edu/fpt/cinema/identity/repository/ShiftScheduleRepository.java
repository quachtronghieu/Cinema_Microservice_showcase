package vn.edu.fpt.cinema.identity.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.identity.domain.entity.ShiftSchedule;

@Repository
public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, String> {
    List<ShiftSchedule> findByStaffId(String staffId);
    List<ShiftSchedule> findByShiftDate(LocalDate shiftDate);
}
