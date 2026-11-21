package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cinema.booking.entity.IdempotencyRecord;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.IdempotencyRecordRepository;

/** Durable Idempotency-Key handling (AGENTS 5.4): results live in booking_db for at least 24 hours. */
@Service
public class IdempotencyService {
    static final Duration RETENTION = Duration.ofHours(24);
    /** Printable ASCII without spaces, 1-128 characters (AGENTS 23). */
    private static final Pattern KEY = Pattern.compile("[\\x21-\\x7E]{1,128}");

    private final IdempotencyRecordRepository records;
    private final ObjectMapper json;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper json, Clock clock) {
        this.records = records;
        this.json = json;
        this.clock = clock;
    }

    public static void validateKey(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new BookingException(BAD_REQUEST, "IDEMPOTENCY_KEY_INVALID",
                "Idempotency-Key header is required: 1-128 printable ASCII characters without spaces.");
        }
    }

    /** Stored result for the same key and payload; 409 when the key was used for a different payload. */
    public <T> Optional<T> replay(String actorId, String operation, String key, String requestHash, Class<T> type) {
        Instant now = clock.instant();
        records.deleteExpiredKey(actorId, operation, key, now);
        return records.findByActorIdAndOperationAndIdemKey(actorId, operation, key)
            .filter(record -> record.getExpiresAt().isAfter(now))
            .map(record -> {
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new BookingException(CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "This Idempotency-Key was already used for a different request.");
                }
                try { return json.readValue(record.getResponseBody(), type); }
                catch (JsonProcessingException e) { throw new IllegalStateException("Corrupt idempotency record", e); }
            });
    }

    /** Must run inside the business transaction: the unique key decides which concurrent request wins. */
    public void record(String actorId, String operation, String key, String requestHash, String resourceId,
                       int status, Object response) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        try {
            records.saveAndFlush(new IdempotencyRecord(actorId, operation, key, requestHash, resourceId, status,
                json.writeValueAsString(response), now, now.plus(RETENTION)));
        } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    public int purgeExpired() { return records.deleteExpired(clock.instant()); }
}
