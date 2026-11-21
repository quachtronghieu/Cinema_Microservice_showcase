package vn.edu.fpt.cinema.booking.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;
import vn.edu.fpt.cinema.booking.validation.ValidVoucherDiscount;

@ValidVoucherDiscount
public record CreateVoucherRequest(
    @JsonDeserialize(using = VoucherTextDeserializer.class) @NotBlank @Size(max = 20) String code,
    @JsonDeserialize(using = VoucherTextDeserializer.class) @NotBlank @Size(max = 255) String voucherName,
    @NotNull VoucherDiscountType discountType,
    @NotNull @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2) BigDecimal discountValue,
    @NotNull @Min(0) Integer usageLimit
) {
    public CreateVoucherRequest {
        code = normalizeCode(code);
        voucherName = trim(voucherName);
        discountValue = normalizeMoney(discountValue);
    }

    static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
