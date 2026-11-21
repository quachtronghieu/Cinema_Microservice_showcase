package vn.edu.fpt.cinema.catalog.concession.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class ApprovedImageValidator implements ConstraintValidator<ApprovedImage, String> {
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || ConcessionImages.contains(value);
    }
}
