package vn.edu.fpt.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Voucher rows are managed by the voucher admin feature; booking reads them and reserves quota. */
@Entity
@Table(name = "Voucher")
public class Voucher {
    @Id @Column(name = "voucher_id") private String voucherId;
    @Column(name = "code", nullable = false) private String code;
    @Column(name = "voucher_name", nullable = false, length = 255) private String voucherName;
    @Column(name = "discount_type", nullable = false) private String discountType;
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2) private BigDecimal discountValue;
    @Column(name = "usage_limit", nullable = false) private int usageLimit;
    @Column(name = "used_count", nullable = false) private int usedCount;
    @Column(name = "reserved_count", nullable = false) private int reservedCount;
    @Column(name = "status", nullable = false) private boolean status;
    // Informational counter maintained by Booking/ADMIN SQL commands, not JPA optimistic locking.
    @Column(name = "version", nullable = false, insertable = false, updatable = false) private long version;

    protected Voucher() {}

    public Voucher(String voucherId, String code, String discountType, BigDecimal discountValue,
                   int usageLimit, int usedCount, int reservedCount, boolean status) {
        this(voucherId, code, code, discountType, discountValue, usageLimit, usedCount, reservedCount, status);
    }

    public Voucher(String voucherId, String code, String voucherName, String discountType, BigDecimal discountValue,
                   int usageLimit, int usedCount, int reservedCount, boolean status) {
        this.voucherId = voucherId;
        this.code = code;
        this.voucherName = voucherName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.reservedCount = reservedCount;
        this.status = status;
    }

    public String getVoucherId() { return voucherId; }
    public String getCode() { return code; }
    public String getVoucherName() { return voucherName; }
    public String getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public int getUsageLimit() { return usageLimit; }
    public int getUsedCount() { return usedCount; }
    public int getReservedCount() { return reservedCount; }
    public int getRemainingCount() { return usageLimit - usedCount - reservedCount; }
    public boolean isStatus() { return status; }
    public long getVersion() { return version; }

    public boolean hasQuotaLeft() { return status && usedCount + reservedCount < usageLimit; }

    /** Discount never exceeds the subtotal; percentages round HALF_UP to 2 decimals (AGENTS 5.3). */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal discount = "PERCENTAGE".equals(discountType)
            ? subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : discountValue.setScale(2, RoundingMode.HALF_UP);
        return discount.min(subtotal);
    }
}
