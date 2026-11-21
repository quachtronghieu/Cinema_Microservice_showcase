package vn.edu.fpt.cinema.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customer")
public class Customer {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", nullable = false, length = 100, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "sex", nullable = false)
    private Boolean sex;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "verify", length = 255)
    private String verify;

    @Column(name = "reset_requested_at")
    private LocalDateTime resetRequestedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Customer() {}

    public Customer(String userId, String username, String passwordHash, LocalDate dob, Boolean sex, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.dob = dob;
        this.sex = sex;
        this.email = email;
        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public Boolean getSex() { return sex; }
    public void setSex(Boolean sex) { this.sex = sex; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVerify() { return verify; }
    public void setVerify(String verify) { this.verify = verify; }

    public LocalDateTime getResetRequestedAt() { return resetRequestedAt; }
    public void setResetRequestedAt(LocalDateTime resetRequestedAt) { this.resetRequestedAt = resetRequestedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
