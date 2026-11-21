package vn.edu.fpt.cinema.booking.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.fpt.cinema.booking.dto.CreateVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;
import vn.edu.fpt.cinema.booking.dto.EditVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.VoucherDiscountType;
import vn.edu.fpt.cinema.booking.dto.VoucherResponse;
import vn.edu.fpt.cinema.booking.entity.Voucher;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.VoucherRepository;
import vn.edu.fpt.cinema.booking.support.BookingTestData;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;

class AdminVoucherServiceIT extends SqlServerIT {
    @Autowired AdminVoucherService admin;
    @Autowired VoucherRepository vouchers;
    @Autowired BookingService bookings;
    @Autowired PaymentService payments;
    @Autowired BookingReleaseService release;
    @Autowired TransactionTemplate transaction;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @BeforeEach void reset() { DbReset.clean(jdbc); }

    private void seed(String id, int limit, int used, int reserved) {
        jdbc.update("""
            INSERT INTO dbo.Voucher
              (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count)
            VALUES (?, ?, N'Voucher name', 'FIXED', 10000, ?, ?, ?)
            """, id, id, limit, used, reserved);
    }

    private EditVoucherRequest patch(String json) {
        try { return mapper.readValue(json, EditVoucherRequest.class); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    private CreateVoucherRequest create(String code) {
        return new CreateVoucherRequest(code, "  Name  ", VoucherDiscountType.PERCENTAGE, new BigDecimal("10.000"), 100);
    }

    private void fails(String code, Runnable command) {
        assertThatThrownBy(command::run).isInstanceOfSatisfying(BookingException.class,
            e -> assertThat(e.code()).isEqualTo(code));
    }

    private <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<T>>();
            for (var command : List.of(first, second)) futures.add(pool.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start gate timed out");
                return command.call();
            }));
            start.countDown();
            var results = new ArrayList<T>();
            for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally { pool.shutdownNow(); }
    }

    private String outcome(Runnable command) {
        try { command.run(); return "OK"; }
        catch (BookingException e) { return e.code(); }
    }

    @Test void createPersistsNormalizedInputAndServerOwnedDefaults() {
        var response = admin.create(create(" hieu10 "));
        assertThat(UUID.fromString(response.voucherId()).toString()).isEqualTo(response.voucherId());
        assertThat(response.code()).isEqualTo("HIEU10");
        assertThat(response.voucherName()).isEqualTo("Name");
        assertThat(response.status()).isTrue();
        assertThat(response.version()).isZero();
        assertThat(response.usedCount()).isZero();
        assertThat(response.reservedCount()).isZero();
        assertThat(response.remainingCount()).isEqualTo(100);
        assertThat(response.discountValue()).isEqualByComparingTo("10.00");
        assertThat(admin.detail(response.voucherId())).isEqualTo(response);
    }

    @Test void disabledCaseInsensitiveDuplicateCodeReturnsConflict() {
        seed("FPT", 10, 0, 0);
        admin.disable("FPT");
        fails("VOUCHER_CODE_EXISTS", () -> admin.create(create(" fpt ")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Voucher", Integer.class)).isEqualTo(1);
    }

    @Test void duplicateCodeDuringEditRollsBackOtherChangesAndVersion() {
        seed("VC1", 10, 0, 0);
        seed("VC2", 10, 0, 0);
        var before = admin.detail("VC2");
        fails("VOUCHER_CODE_EXISTS", () -> admin.edit("VC2", patch("{\"code\":\"vc1\",\"voucherName\":\"Changed\"}")));
        assertThat(admin.detail("VC2")).isEqualTo(before);
    }

    @Test void listIncludesDisabledExhaustedAndEveryPageInIdOrder() {
        for (int i = 0; i < 21; i++) seed("VC%02d".formatted(i), 1, i == 0 ? 1 : 0, 0);
        admin.disable("VC01");
        var first = admin.list(null, 0, 20);
        var second = admin.list(null, 1, 20);
        assertThat(first.totalElements()).isEqualTo(21);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items()).hasSize(20);
        assertThat(second.items()).extracting(VoucherResponse::voucherId).containsExactly("VC20");
        assertThat(first.items().getFirst().remainingCount()).isZero();
        assertThat(first.items().get(1).status()).isFalse();
        assertThat(admin.list(false, 0, 20).items()).extracting(VoucherResponse::voucherId).containsExactly("VC01");
        assertThat(admin.list(true, 0, 20).totalElements()).isEqualTo(20);
        assertThat(admin.list(null, 2, 20).items()).isEmpty();
    }

    @Test void emptyListHasZeroPages() {
        assertThat(admin.list(null, 0, 20).totalPages()).isZero();
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,101", "0,-1"})
    void invalidPaginationRejected(int page, int size) {
        fails("VALIDATION_ERROR", () -> admin.list(null, page, size));
    }

    @Test void missingLegacyIdIsNotRejectedAsAnInvalidUuid() {
        fails("VOUCHER_NOT_FOUND", () -> admin.detail("VC-old"));
        fails("VOUCHER_NOT_FOUND", () -> admin.edit("VC-old", patch("{\"usageLimit\":10}")));
        fails("VOUCHER_NOT_FOUND", () -> admin.disable("VC-old"));
    }

    @Test void actualMultiFieldEditIncrementsOnceAndOnlyTouchesSuppliedFields() {
        seed("VC1", 10, 0, 0);
        var response = admin.edit("VC1", patch("{\"voucherName\":\"New name\",\"usageLimit\":20}"));
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.voucherName()).isEqualTo("New name");
        assertThat(response.usageLimit()).isEqualTo(20);
        assertThat(response.code()).isEqualTo("VC1");
        assertThat(response.discountValue()).isEqualByComparingTo("10000");
    }

    @Test void protectedFieldsMayChangeBeforeUse() {
        seed("VC1", 10, 0, 0);
        var response = admin.edit("VC1", patch("{\"code\":\"new10\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10}"));
        assertThat(response.code()).isEqualTo("NEW10");
        assertThat(response.discountType()).isEqualTo(VoucherDiscountType.PERCENTAGE);
        assertThat(response.discountValue()).isEqualByComparingTo("10");
        assertThat(response.version()).isEqualTo(1);
    }

    @ParameterizedTest @CsvSource({"1,0", "0,1"})
    void protectedChangesWithUsedOrReservedRejectEntireCommand(int used, int reserved) {
        seed("VC1", 10, used, reserved);
        var before = admin.detail("VC1");
        for (String field : List.of("\"code\":\"NEW\"", "\"discountType\":\"PERCENTAGE\",\"discountValue\":10", "\"discountValue\":20000")) {
            fails("VOUCHER_EDIT_RESTRICTED", () -> admin.edit("VC1", patch("{" + field + ",\"voucherName\":\"Changed\"}")));
            assertThat(admin.detail("VC1")).isEqualTo(before);
        }
    }

    @Test void unchangedProtectedFieldsRemainANoOpWithReservations() {
        seed("VC1", 10, 0, 1);
        var before = admin.detail("VC1");
        assertThat(admin.edit("VC1", patch("{\"code\":\"vc1\",\"discountType\":\"FIXED\",\"discountValue\":10000.000}")))
            .isEqualTo(before);
    }

    @Test void mergedTypeValueValidationRejectsInvalidSingleFieldEdit() {
        seed("VC1", 10, 0, 0);
        assertThatThrownBy(() -> admin.edit("VC1", patch("{\"discountType\":\"PERCENTAGE\"}")))
            .isInstanceOf(ConstraintViolationException.class);
        admin.edit("VC1", patch("{\"discountType\":\"PERCENTAGE\",\"discountValue\":10}"));
        var before = admin.detail("VC1");
        assertThatThrownBy(() -> admin.edit("VC1", patch("{\"discountValue\":101}")))
            .isInstanceOf(ConstraintViolationException.class);
        assertThat(admin.detail("VC1")).isEqualTo(before);
    }

    @Test void nameStatusAndValidLimitCanChangeWithUsesWithoutOverwritingCounters() {
        seed("VC1", 100, 70, 20);
        var response = admin.edit("VC1", patch("{\"voucherName\":\"New\",\"status\":false,\"usageLimit\":90}"));
        assertThat(response.usedCount()).isEqualTo(70);
        assertThat(response.reservedCount()).isEqualTo(20);
        assertThat(response.remainingCount()).isZero();
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.status()).isFalse();
    }

    @Test void limitBelowQuotaRejectsWholeCommandAndExactQuotaIsAllowed() {
        seed("VC1", 100, 70, 20);
        var before = admin.detail("VC1");
        fails("VOUCHER_LIMIT_CONFLICT", () -> admin.edit("VC1", patch("{\"usageLimit\":89,\"status\":false}")));
        assertThat(admin.detail("VC1")).isEqualTo(before);
        assertThat(admin.edit("VC1", patch("{\"usageLimit\":90}" )).remainingCount()).isZero();
    }

    @Test void disableRepeatDoesNotIncrementAndReenableDoesNotResetQuota() {
        seed("VC1", 2, 1, 1);
        admin.disable("VC1");
        var disabled = admin.detail("VC1");
        assertThat(disabled.status()).isFalse();
        assertThat(disabled.version()).isEqualTo(1);
        admin.disable("VC1");
        assertThat(admin.detail("VC1")).isEqualTo(disabled);
        var enabled = admin.edit("VC1", patch("{\"status\":true}"));
        assertThat(enabled.status()).isTrue();
        assertThat(enabled.version()).isEqualTo(2);
        assertThat(enabled.remainingCount()).isZero();
    }

    @Test void invalidDirectServiceRequestsDoNotWrite() {
        assertThatThrownBy(() -> admin.create(new CreateVoucherRequest("", "", VoucherDiscountType.FIXED, BigDecimal.ZERO, -1)))
            .isInstanceOf(ConstraintViolationException.class);
        seed("VC1", 10, 0, 0);
        assertThatThrownBy(() -> admin.edit("VC1", patch("{}"))).isInstanceOf(ConstraintViolationException.class);
        assertThat(admin.detail("VC1").version()).isZero();
    }

    @Test void adminReadDoesNotTrustAnEntityCachedBeforeNativeReservation() {
        seed("VC1", 10, 0, 0);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            assertThat(entityManager.find(Voucher.class, "VC1").getReservedCount()).isZero();
            assertThat(vouchers.reserveOne("VC1")).isEqualTo(1);
            admin.edit("VC1", patch("{\"discountValue\":20000}"));
        })).isInstanceOfSatisfying(BookingException.class, e -> assertThat(e.code()).isEqualTo("VOUCHER_EDIT_RESTRICTED"));
        assertThat(admin.detail("VC1").reservedCount()).isZero(); // whole failed transaction rolled back
    }

    @Test void concurrentSameFieldCommandsReturnTheirSerializedVersionsAndLastWins() throws Exception {
        seed("VC1", 10, 0, 0);
        var results = concurrently(() -> admin.edit("VC1", patch("{\"usageLimit\":20}")),
            () -> admin.edit("VC1", patch("{\"usageLimit\":30}")));
        assertThat(results).extracting(VoucherResponse::version).containsExactlyInAnyOrder(1L, 2L);
        var last = results.stream().filter(r -> r.version() == 2).findFirst().orElseThrow();
        assertThat(admin.detail("VC1")).isEqualTo(last);
    }

    @Test void concurrentDifferentFieldsPreserveBothChanges() throws Exception {
        seed("VC1", 10, 0, 0);
        concurrently(() -> admin.edit("VC1", patch("{\"voucherName\":\"Changed\"}")),
            () -> admin.edit("VC1", patch("{\"usageLimit\":30}")));
        var result = admin.detail("VC1");
        assertThat(result.voucherName()).isEqualTo("Changed");
        assertThat(result.usageLimit()).isEqualTo(30);
        assertThat(result.version()).isEqualTo(2);
    }

    @Test void concurrentDuplicateCreatesYieldOneCodeConflict() throws Exception {
        var results = concurrently(() -> outcome(() -> admin.create(create(" fpt "))),
            () -> outcome(() -> admin.create(create("FPT"))));
        assertThat(results).containsExactlyInAnyOrder("OK", "VOUCHER_CODE_EXISTS");
        assertThat(admin.list(null, 0, 20).totalElements()).isEqualTo(1);
    }

    @Test void limitDecreaseVersusReservationPreservesQuota() throws Exception {
        seed("VC1", 1, 0, 0);
        var results = concurrently(() -> outcome(() -> admin.edit("VC1", patch("{\"usageLimit\":0}"))),
            () -> "RESERVE_" + transaction.execute(status -> vouchers.reserveOne("VC1")));
        assertThat(results).isIn(List.of("OK", "RESERVE_0"), List.of("VOUCHER_LIMIT_CONFLICT", "RESERVE_1"));
        var row = admin.detail("VC1");
        assertThat(row.usedCount() + row.reservedCount()).isLessThanOrEqualTo(row.usageLimit());
        assertThat(row.version()).isEqualTo(1);
    }

    @Test void protectedEditVersusReservationUsesCurrentCounters() throws Exception {
        seed("VC1", 1, 0, 0);
        var results = concurrently(() -> outcome(() -> admin.edit("VC1", patch("{\"discountValue\":20000}"))),
            () -> "RESERVE_" + transaction.execute(status -> vouchers.reserveOne("VC1")));
        assertThat(results).isIn(List.of("OK", "RESERVE_1"), List.of("VOUCHER_EDIT_RESTRICTED", "RESERVE_1"));
        assertThat(admin.detail("VC1").discountValue()).isEqualByComparingTo(results.getFirst().equals("OK") ? "20000" : "10000");
    }

    @Test void disableVersusReservationSerializesWithoutLosingReservedUse() throws Exception {
        seed("VC1", 1, 0, 0);
        var results = concurrently(() -> outcome(() -> admin.disable("VC1")),
            () -> "RESERVE_" + transaction.execute(status -> vouchers.reserveOne("VC1")));
        var row = admin.detail("VC1");
        assertThat(row.status()).isFalse();
        assertThat(row.reservedCount()).isEqualTo(results.get(1).equals("RESERVE_1") ? 1 : 0);
        assertThat(row.version()).isEqualTo(1 + row.reservedCount());
        assertThat(transaction.<Integer>execute(status -> vouchers.reserveOne("VC1"))).isZero();
    }

    private String bookingWithVoucher() {
        seed("VC1", 1, 0, 0);
        BookingTestData.showtimeWithFourSeats(jdbc);
        BookingTestData.hold(jdbc, "H1", "CS1", Instant.now().plusSeconds(300), "SS1");
        return bookings.create("CS1", "key-1", new CustomerBookingRequest("H1", "SW1", List.of("SS1"), List.of(), "VC1"))
            .body().bookingId();
    }

    @Test void reservedBookingStillPaysAfterAdminDisablesVoucher() {
        String id = bookingWithVoucher();
        admin.disable("VC1");
        assertThat(payments.pay("CS1", id, "SUCCESS").paymentStatus()).isEqualTo("SUCCEEDED");
        var row = admin.detail("VC1");
        assertThat(row.status()).isFalse();
        assertThat(row.usedCount()).isEqualTo(1);
        assertThat(row.reservedCount()).isZero();
        assertThat(row.version()).isEqualTo(3); // reserve, ADMIN disable, payment
        assertThat(jdbc.queryForObject("SELECT total_amount FROM dbo.Booking WHERE booking_id = ?", BigDecimal.class, id))
            .isEqualByComparingTo("70000");
    }

    @Test void releaseAllUnusedReservationsAllowsReeditWithoutChangingOldBooking() {
        String id = bookingWithVoucher();
        fails("VOUCHER_EDIT_RESTRICTED", () -> admin.edit("VC1", patch("{\"discountValue\":20000}")));
        assertThat(release.release(id, "CANCELLED")).isTrue();
        var row = admin.edit("VC1", patch("{\"discountValue\":20000}"));
        assertThat(row.usedCount()).isZero();
        assertThat(row.reservedCount()).isZero();
        assertThat(row.discountValue()).isEqualByComparingTo("20000");
        assertThat(jdbc.queryForObject("SELECT total_amount FROM dbo.Booking WHERE booking_id = ?", BigDecimal.class, id))
            .isEqualByComparingTo("70000");
    }
}
