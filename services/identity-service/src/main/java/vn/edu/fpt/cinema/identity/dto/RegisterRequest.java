package vn.edu.fpt.cinema.identity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class RegisterRequest {

    @NotBlank(message = "The username is required.")
    @Size(min = 2, max = 20, message = "The name must have between 2 and 20 characters.")
    private String username;

    @NotBlank(message = "A password is required.")
    @Size(min = 6, message = "The password must be at least 6 characters long.")
    private String password;

    @NotBlank(message = "Confirmation password is required.")
    private String confirmPassword;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email.")
    private String email;

    @NotBlank(message = "The phone number is required.")
    private String phone;

    private String dob;

    @NotNull(message = "Please select a gender.")
    private Boolean sex;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String confirmPassword, String email, String phone, String dob, Boolean sex) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.sex = sex;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public Boolean getSex() { return sex; }
    public void setSex(Boolean sex) { this.sex = sex; }
}
