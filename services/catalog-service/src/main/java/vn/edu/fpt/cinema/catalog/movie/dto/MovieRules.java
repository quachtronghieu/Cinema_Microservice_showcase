package vn.edu.fpt.cinema.catalog.movie.dto;

// Format rules carried over from the monolith's MovieRequestDTO. Text columns are
// VARCHAR, so non-ASCII titles would be corrupted on write and stay rejected.
final class MovieRules {
    static final String TITLE = "[A-Za-z0-9 .,:;!?'\"()\\-]+";
    static final String TITLE_MESSAGE = "Title can only contain letters, numbers, spaces and basic punctuation";
    static final String GENRE = "[A-Za-z0-9 ,/\\-]+";
    static final String GENRE_MESSAGE = "Genre can only contain letters, numbers, spaces, commas, slashes and hyphens";
    static final String SUMMARY = "[A-Za-z0-9\\s.,:;!?'\"()\\-]+";
    static final String SUMMARY_MESSAGE = "Summary can only contain letters, numbers, spaces, basic punctuation and line breaks";
    static final int MIN_DURATION = 61;
    static final int MAX_DURATION = 180;
    static final String DURATION_MESSAGE = "Duration must be from 61 to 180 minutes";
    static final String AGE_RATING = "P|T13|T16|T18";
    static final String AGE_RATING_MESSAGE = "Age rating must be one of P, T13, T16, T18";
    static final String TRAILER = "(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[A-Za-z0-9_-]{11}(&\\S*)?";
    static final String TRAILER_MESSAGE = "Trailer must be a valid YouTube link";
    private MovieRules() {}
}
