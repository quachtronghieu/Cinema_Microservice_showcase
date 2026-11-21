package vn.edu.fpt.cinema.identity.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    // Store token / JTI -> expiration timestamp in seconds
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revoke(String identifier, long expiresAtEpochSecond) {
        if (identifier != null && !identifier.isBlank()) {
            blacklist.put(identifier, expiresAtEpochSecond);
        }
    }

    public boolean isRevoked(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        Long expiresAt = blacklist.get(identifier);
        if (expiresAt == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (now > expiresAt) {
            blacklist.remove(identifier);
            return false;
        }
        return true;
    }

    public void cleanupExpiredTokens() {
        long now = Instant.now().getEpochSecond();
        blacklist.entrySet().removeIf(entry -> now > entry.getValue());
    }

    public int getBlacklistSize() {
        return blacklist.size();
    }
}
