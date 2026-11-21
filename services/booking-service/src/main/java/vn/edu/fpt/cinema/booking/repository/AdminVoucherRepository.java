package vn.edu.fpt.cinema.booking.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.cinema.booking.dto.CreateVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.VoucherDiscountType;
import vn.edu.fpt.cinema.booking.dto.VoucherPage;
import vn.edu.fpt.cinema.booking.dto.VoucherResponse;

/** ADMIN SQL shares the Voucher row locks and counters used by Booking's native commands. */
@Repository
public class AdminVoucherRepository {
    private static final Set<String> EDITABLE_COLUMNS = Set.of(
        "code", "voucher_name", "discount_type", "discount_value", "usage_limit", "status");
    private final JdbcTemplate jdbc;

    public AdminVoucherRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // JDBC sends String parameters as NVARCHAR; against the VARCHAR key that forces an index scan, which reads
    // other transactions' uncommitted rows and deadlocked two concurrent creates. CAST keeps it a key seek.
    private static final String BY_ID = " WHERE voucher_id = CAST(? AS VARCHAR(36))";

    public Optional<VoucherResponse> find(String id) {
        return jdbc.query("SELECT * FROM dbo.Voucher" + BY_ID, this::row, id).stream().findFirst();
    }

    /** Must be called within the application's write transaction; bypasses stale JPA entities. */
    public Optional<VoucherResponse> lock(String id) {
        return jdbc.query("SELECT * FROM dbo.Voucher WITH (UPDLOCK, ROWLOCK)" + BY_ID,
            this::row, id).stream().findFirst();
    }

    public VoucherPage list(Boolean status, int page, int size) {
        String where = status == null ? "" : " WHERE status = ?";
        Object[] filter = status == null ? new Object[0] : new Object[] {status};
        long count = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Voucher" + where, Long.class, filter);
        var arguments = new ArrayList<Object>();
        if (status != null) arguments.add(status);
        arguments.add((long) page * size);
        arguments.add(size);
        var items = jdbc.query("SELECT * FROM dbo.Voucher" + where
            + " ORDER BY voucher_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", this::row, arguments.toArray());
        return new VoucherPage(items, page, size, count, Math.toIntExact((count + size - 1) / size));
    }

    public void insert(String id, CreateVoucherRequest request) {
        jdbc.update("""
            INSERT INTO dbo.Voucher
              (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit)
            VALUES (?, ?, ?, ?, ?, ?)
            """, id, request.code(), request.voucherName(), request.discountType().name(),
            request.discountValue(), request.usageLimit());
    }

    /** One changed command increments version once; counters are never writable ADMIN fields. */
    public int update(String id, Map<String, Object> changes, int resultingLimit, boolean protectedEdit) {
        if (changes.isEmpty()) return 0;
        if (!EDITABLE_COLUMNS.containsAll(changes.keySet())) throw new IllegalArgumentException("Invalid ADMIN column");
        var assignments = new ArrayList<String>();
        var arguments = new ArrayList<Object>();
        changes.forEach((column, value) -> {
            assignments.add(column + " = ?");
            arguments.add(value);
        });
        arguments.add(id);
        arguments.add(resultingLimit);
        return jdbc.update("UPDATE dbo.Voucher SET " + String.join(", ", assignments)
            + ", version = version + 1" + BY_ID + " AND used_count + reserved_count <= ?"
            + (protectedEdit ? " AND used_count = 0 AND reserved_count = 0" : ""), arguments.toArray());
    }

    private VoucherResponse row(ResultSet rs, int rowNumber) throws SQLException {
        int limit = rs.getInt("usage_limit");
        int used = rs.getInt("used_count");
        int reserved = rs.getInt("reserved_count");
        return new VoucherResponse(rs.getString("voucher_id"), rs.getString("code"), rs.getString("voucher_name"),
            VoucherDiscountType.valueOf(rs.getString("discount_type")), rs.getBigDecimal("discount_value"),
            limit, used, reserved, limit - used - reserved, rs.getBoolean("status"), rs.getLong("version"));
    }
}
