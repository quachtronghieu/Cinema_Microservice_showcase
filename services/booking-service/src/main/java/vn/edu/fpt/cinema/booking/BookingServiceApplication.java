package vn.edu.fpt.cinema.booking;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookingServiceApplication {
    public static void main(String[] args) {
        // UTC JVM: instants stay UTC and showtime LocalTime/LocalDate values are read without a zone shift
        // (a +7 JVM read TIME 19:00 back as 03:00 because Hibernate converts via 1970-01-01, when Vietnam was +8).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
