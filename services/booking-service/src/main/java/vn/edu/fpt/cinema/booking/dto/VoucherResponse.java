package vn.edu.fpt.cinema.booking.dto;

import java.math.BigDecimal;
import vn.edu.fpt.cinema.booking.entity.Voucher;

public record VoucherResponse(
    String voucherId, String code, String voucherName, VoucherDiscountType discountType,
    BigDecimal discountValue, int usageLimit, int usedCount, int reservedCount,
    int remainingCount, boolean status, long version
) {
    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(voucher.getVoucherId(), voucher.getCode(), voucher.getVoucherName(),
            VoucherDiscountType.valueOf(voucher.getDiscountType()), voucher.getDiscountValue(),
            voucher.getUsageLimit(), voucher.getUsedCount(), voucher.getReservedCount(),
            voucher.getRemainingCount(), voucher.isStatus(), voucher.getVersion());
    }
}
