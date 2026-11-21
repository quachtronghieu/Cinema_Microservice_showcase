package vn.edu.fpt.cinema.identity.application;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.domain.entity.PasswordResetToken;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.entity.Staff;
import vn.edu.fpt.cinema.identity.dto.ForgotPasswordRequest;
import vn.edu.fpt.cinema.identity.dto.ResetPasswordRequest;
import vn.edu.fpt.cinema.identity.repository.CustomerRepository;
import vn.edu.fpt.cinema.identity.repository.PasswordResetTokenRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.repository.StaffRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String LEGACY_TOKEN_SECRET = "wait";

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final PrincipalRepository principalRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:your-email@example.com}")
    private String mailFrom;

    public PasswordResetService(
            CustomerRepository customerRepository,
            StaffRepository staffRepository,
            PrincipalRepository principalRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.principalRepository = principalRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Transactional
    public Map<String, Object> requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        Staff staff = (customer == null) ? staffRepository.findByEmail(email).orElse(null) : null;

        if (customer == null && staff == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for this email.");
        }

        Principal principal;
        String domainId;
        String username;

        if (customer != null) {
            domainId = customer.getUserId();
            username = customer.getUsername();
            principal = principalRepository.findByCustomerId(domainId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Principal mapping not found for customer."));
        } else {
            domainId = staff.getStaffId();
            username = staff.getUsername();
            principal = principalRepository.findByStaffId(domainId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Principal mapping not found for staff."));
        }

        // Generate unique secure reset token
        long nowMillis = System.currentTimeMillis();
        String rawToken = UUID.randomUUID().toString().replace("-", "") + Long.toHexString(nowMillis);
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // Store into Password_Reset_Token table
        PasswordResetToken resetToken = new PasswordResetToken(
                UUID.randomUUID().toString(),
                tokenHash,
                principal,
                expiresAt
        );
        tokenRepository.save(resetToken);

        // Update legacy customer fields if customer
        if (customer != null) {
            customer.setVerify("resetPassword");
            customer.setResetRequestedAt(LocalDateTime.now());
            customerRepository.save(customer);
        }

        String resetLink = "http://localhost:5173/reset-password/" + domainId + "/" + nowMillis + "/" + rawToken;
        log.info("\n"
                + "================================================================================\n"
                + "[EMAIL SERVICE] Password Reset Email Dispatched\n"
                + "To: {}\n"
                + "Subject: Reset Your Password - CGV Movies / Cinema Management\n"
                + "Username: {}\n"
                + "Reset Deep-Link: {}\n"
                + "Expires in: 10 minutes (600 seconds)\n"
                + "================================================================================",
                email, username, resetLink);

        // Send real email if mailSender is available
        sendRealEmail(email, username, resetLink);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Please check your email to change password.");
        response.put("email", email);
        response.put("resetToken", rawToken);
        response.put("resetLink", resetLink);
        response.put("expiresInSeconds", 600);

        return response;
    }

    private void sendRealEmail(String toEmail, String username, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[EMAIL SERVICE] No JavaMailSender available, email simulated in console.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - Cinema Management");

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;\">"
                    + "<h2 style=\"color: #E50914; text-align: center;\">CINEMA MANAGEMENT</h2>"
                    + "<p>Xin chào <strong>" + username + "</strong>,</p>"
                    + "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này.</p>"
                    + "<p>Vui lòng nhấn vào liên kết bên dưới để tạo mật khẩu mới (liên kết có hiệu lực trong <strong>10 phút</strong>):</p>"
                    + "<p style=\"text-align: center; margin: 25px 0;\">"
                    + "<a href=\"" + resetLink + "\" style=\"background-color: #E50914; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;\">Đặt lại mật khẩu</a>"
                    + "</p>"
                    + "<p>Hoặc bạn có thể sao chép liên kết sau vào trình duyệt hoặc ứng dụng di động:</p>"
                    + "<p style=\"word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 4px; font-size: 13px;\">" + resetLink + "</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #888;\">Nếu bạn không yêu cầu hành động này, vui lòng bỏ qua email này.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[EMAIL SERVICE] Real email sent successfully to {} via SMTP", toEmail);
        } catch (Exception e) {
            log.warn("[EMAIL SERVICE] Failed to send real email to {}: {}. Token still available in console/API response.", toEmail, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        // Validate password input
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password must be at least 6 characters long.");
        }
        if (request.getConfirmPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The confirmation passwords do not match.");
        }

        String rawToken = request.getToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required.");
        }

        // Look up token by hash
        String tokenHash = hashToken(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash).orElse(null);

        Principal principal = null;
        if (token != null) {
            if (Boolean.TRUE.equals(token.getUsed())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has already been used.");
            }
            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has expired.");
            }
            principal = token.getPrincipal();
            token.setUsed(true);
            tokenRepository.save(token);
        } else if (request.getId() != null && request.getResetTimestamp() != null) {
            // Support legacy token validation (SHA-256 of TOKEN_SECRET:customerId:timestamp)
            String expectedLegacyToken = createLegacyToken(request.getId(), request.getResetTimestamp());
            if (expectedLegacyToken.equalsIgnoreCase(rawToken)) {
                Customer customer = customerRepository.findById(request.getId()).orElse(null);
                if (customer != null && "resetPassword".equals(customer.getVerify()) && customer.getResetRequestedAt() != null) {
                    if (customer.getResetRequestedAt().plusMinutes(10).isAfter(LocalDateTime.now())) {
                        principal = principalRepository.findByCustomerId(customer.getUserId()).orElse(null);
                    }
                }
            }
        }

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link is no longer valid or has expired.");
        }

        // Update password with BCrypt
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        principal.setPasswordHash(newPasswordHash);
        principalRepository.save(principal);

        // Sync with Customer or Staff entity
        if (principal.getCustomerId() != null) {
            customerRepository.findById(principal.getCustomerId()).ifPresent(c -> {
                c.setPasswordHash(newPasswordHash);
                c.setVerify("active");
                c.setResetRequestedAt(null);
                customerRepository.save(c);
            });
        }
        if (principal.getStaffId() != null) {
            staffRepository.findById(principal.getStaffId()).ifPresent(s -> {
                s.setPasswordHash(newPasswordHash);
                staffRepository.save(s);
            });
        }

        log.info("[PasswordReset] Password reset successfully for principal '{}'", principal.getUsername());
        return Map.of("message", "Password has been reset successfully.");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String createLegacyToken(String customerId, long timestamp) {
        String input = LEGACY_TOKEN_SECRET + ":" + customerId + ":" + timestamp;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
