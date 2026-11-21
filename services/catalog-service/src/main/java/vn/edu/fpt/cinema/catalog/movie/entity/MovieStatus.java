package vn.edu.fpt.cinema.catalog.movie.entity;

import java.util.Optional;

public enum MovieStatus {
    NOW_SHOWING("now-showing"), COMING_SOON("coming-soon");

    private final String param;
    MovieStatus(String param) { this.param = param; }
    public static Optional<MovieStatus> parse(String value) {
        for (var status : values()) if (status.param.equals(value)) return Optional.of(status);
        return Optional.empty();
    }
}
