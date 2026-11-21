package vn.edu.fpt.cinema.booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VoucherDiscountValidator.class)
public @interface ValidVoucherDiscount {
    String message() default "Percentage discount must be at most 100";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
