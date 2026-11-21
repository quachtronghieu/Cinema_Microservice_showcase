package vn.edu.fpt.cinema.catalog.concession.validation;
import jakarta.validation.*;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ApprovedImageValidator.class)
public @interface ApprovedImage {
    String message() default "Choose an approved concession image filename";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
