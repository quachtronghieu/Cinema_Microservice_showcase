package vn.edu.fpt.cinema.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "Shift_Schedule")
public class ShiftSchedule {

    @Id
    @Column(name = "shift_schedule_id", length = 36)
    private String shiftScheduleId;

    @Column(name = "staff_id", nullable = false, length = 36)
    private String staffId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart;

    @Column(name = "late_minute")
    private Integer lateMinute;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ShiftSchedule() {}

    public ShiftSchedule(String shiftScheduleId, String staffId, LocalDate shiftDate, String status, LocalTime shiftStart, Integer lateMinute) {
        this.shiftScheduleId = shiftScheduleId;
        this.staffId = staffId;
        this.shiftDate = shiftDate;
        this.status = status;
        this.shiftStart = shiftStart;
        this.lateMinute = lateMinute;
    }

    public String getShiftScheduleId() { return shiftScheduleId; }
    public void setShiftScheduleId(String shiftScheduleId) { this.shiftScheduleId = shiftScheduleId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalTime shiftStart) { this.shiftStart = shiftStart; }

    public Integer getLateMinute() { return lateMinute; }
    public void setLateMinute(Integer lateMinute) { this.lateMinute = lateMinute; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
