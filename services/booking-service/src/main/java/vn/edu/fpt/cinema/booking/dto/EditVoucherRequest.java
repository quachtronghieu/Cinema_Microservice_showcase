package vn.edu.fpt.cinema.booking.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;
import vn.edu.fpt.cinema.booking.validation.ValidVoucherDiscount;

/** Absent fields stay null; explicit JSON null is rejected by each setter. */
@ValidVoucherDiscount
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class EditVoucherRequest {
    @JsonDeserialize(using = VoucherTextDeserializer.class)
    @Size(min = 1, max = 20) @Pattern(regexp = "(?s).*\\S.*")
    private String code;
    @JsonDeserialize(using = VoucherTextDeserializer.class)
    @Size(min = 1, max = 255) @Pattern(regexp = "(?s).*\\S.*")
    private String voucherName;
    private VoucherDiscountType discountType;
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2)
    private BigDecimal discountValue;
    @Min(0) private Integer usageLimit;
    private Boolean status;

    @JsonSetter(value = "code", nulls = Nulls.FAIL)
    public void setCode(String value) {
        code = CreateVoucherRequest.normalizeCode(Objects.requireNonNull(value));
    }

    @JsonSetter(value = "voucherName", nulls = Nulls.FAIL)
    public void setVoucherName(String value) {
        voucherName = CreateVoucherRequest.trim(Objects.requireNonNull(value));
    }

    @JsonSetter(value = "discountType", nulls = Nulls.FAIL)
    public void setDiscountType(VoucherDiscountType value) {
        discountType = Objects.requireNonNull(value);
    }

    @JsonSetter(value = "discountValue", nulls = Nulls.FAIL)
    public void setDiscountValue(BigDecimal value) {
        discountValue = CreateVoucherRequest.normalizeMoney(Objects.requireNonNull(value));
    }

    @JsonSetter(value = "usageLimit", nulls = Nulls.FAIL)
    public void setUsageLimit(Integer value) {
        usageLimit = Objects.requireNonNull(value);
    }

    @JsonSetter(value = "status", nulls = Nulls.FAIL)
    public void setStatus(Boolean value) {
        status = Objects.requireNonNull(value);
    }

    public String code() { return code; }
    public String voucherName() { return voucherName; }
    public VoucherDiscountType discountType() { return discountType; }
    public BigDecimal discountValue() { return discountValue; }
    public Integer usageLimit() { return usageLimit; }
    public Boolean status() { return status; }

    @AssertTrue(message = "At least one editable field is required")
    public boolean isNotEmpty() {
        return code != null || voucherName != null || discountType != null
            || discountValue != null || usageLimit != null || status != null;
    }
}
