package vn.edu.fpt.cinema.booking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import vn.edu.fpt.cinema.booking.dto.CreateVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.EditVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.VoucherDiscountType;

public class VoucherDiscountValidator implements ConstraintValidator<ValidVoucherDiscount, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        VoucherDiscountType type;
        BigDecimal amount;
        if (value instanceof CreateVoucherRequest request) {
            type = request.discountType();
            amount = request.discountValue();
        } else if (value instanceof EditVoucherRequest request) {
            type = request.discountType();
            amount = request.discountValue();
        } else {
            return true;
        }
        // A partial edit must also be checked against the current row in the application command.
        if (type != VoucherDiscountType.PERCENTAGE || amount == null
                || amount.compareTo(BigDecimal.valueOf(100)) <= 0) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode("discountValue").addConstraintViolation();
        return false;
    }
}
