package vn.edu.fpt.cinema.identity.application;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Server-side check of service credentials and grants before identity issues a service token (AGENTS 25). */
@Service
public class ServiceClientService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public ServiceClientService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    /** Active client whose secret matches the stored BCrypt hash. */
    public boolean authenticate(String clientId, String clientSecret) {
        List<String> hashes = jdbc.queryForList(
            "SELECT client_secret_hash FROM dbo.Service_Client WHERE client_id = ? AND is_active = 1", String.class, clientId);
        return !hashes.isEmpty() && passwordEncoder.matches(clientSecret, hashes.get(0));
    }

    /** The scope a client may ask for comes from its grants, never from the request alone. */
    public boolean hasGrant(String clientId, String targetService, String scope) {
        Integer grants = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.Service_Client_Grant WHERE client_id = ? AND target_service = ? AND scope = ?",
            Integer.class, clientId, targetService, scope);
        return grants != null && grants > 0;
    }
}
