package vn.edu.fpt.cinema.cinema.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.cinema.entities.Showtime;
import vn.edu.fpt.cinema.cinema.entities.ShowtimeSeat;
import vn.edu.fpt.cinema.cinema.entities.TemplateSeat;
import vn.edu.fpt.cinema.cinema.enums.SeatStatus;
import vn.edu.fpt.cinema.cinema.repositories.ShowtimeRepository;
import vn.edu.fpt.cinema.cinema.repositories.ShowtimeSeatRepository;
import vn.edu.fpt.cinema.cinema.repositories.TemplateSeatRepository;

@Service
public class ShowtimeSeatService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TemplateSeatRepository templateSeatRepository;
    private final ShowtimeRepository showtimeRepository;

    public ShowtimeSeatService(
            ShowtimeSeatRepository showtimeSeatRepository,
            TemplateSeatRepository templateSeatRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.templateSeatRepository = templateSeatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public boolean hasSeatsForShowtime(String showtimeId) {
        return showtimeSeatRepository.existsByShowtime_ShowtimeId(showtimeId);
    }

    /** The room's fixed seats, by row (A..Z, then AA..) and seat number. */
    public List<TemplateSeat> physicalSeats(String templateId) {
        return templateSeatRepository.findAllByTemplate_Id(templateId).stream()
                .sorted(Comparator.comparingInt((TemplateSeat seat) -> seat.getRowLabel().length())
                        .thenComparing(TemplateSeat::getRowLabel)
                        .thenComparingInt(TemplateSeat::getSeatNumber))
                .toList();
    }

    public List<ShowtimeSeat> getSeatsEntityByShowtimeId(String showtimeId) {
        return showtimeSeatRepository.getAllByShowtime_ShowtimeId(showtimeId);
    }

    @Transactional
    public void createShowtimeSeats(String showtimeId, String templateId) {
        Showtime showtime = showtimeRepository.findByShowtimeId(showtimeId);
        if (showtime == null) return;

        List<TemplateSeat> templateSeats = templateSeatRepository.findAllByTemplate_Id(templateId);
        for (TemplateSeat templateSeat : templateSeats) {
            ShowtimeSeat seat = new ShowtimeSeat();
            seat.setShowtimeSeatID(java.util.UUID.randomUUID().toString());
            seat.setShowtime(showtime);
            seat.setTemplateSeat(templateSeat);
            seat.setStatus(SeatStatus.AVAILABLE);
            showtimeSeatRepository.save(seat);
        }
    }

    @Transactional
    public boolean updateSeatStatus(String showtimeId, List<String> seatIds, SeatStatus newStatus) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdAndIdsForUpdate(showtimeId, seatIds);
        if (seats.size() != seatIds.size()) {
            return false;
        }
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(newStatus);
        }
        showtimeSeatRepository.saveAll(seats);
        return true;
    }
}

