package vn.edu.fpt.cinema.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.fpt.cinema.booking.repository.VoucherRepository;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;

class VoucherMappingIT extends SqlServerIT {
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaction;
    @Autowired VoucherRepository vouchers;

    @BeforeEach void emptyDatabase() {
        DbReset.clean(jdbc);
    }

    @Test void namedVoucherRoundTripsExistingColumnsAndDatabaseVersionDefault() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new Voucher("VC1", "FPT", "FPT discount", "PERCENTAGE",
                new BigDecimal("10.00"), 100, 2, 3, true));
            entityManager.flush();
            entityManager.clear();

            Voucher loaded = entityManager.find(Voucher.class, "VC1");
            assertThat(loaded.getVoucherName()).isEqualTo("FPT discount");
            assertThat(loaded.getUsageLimit()).isEqualTo(100);
            assertThat(loaded.getUsedCount()).isEqualTo(2);
            assertThat(loaded.getReservedCount()).isEqualTo(3);
            assertThat(loaded.getRemainingCount()).isEqualTo(95);
            assertThat(loaded.isStatus()).isTrue();
            assertThat(loaded.getVersion()).isZero();
        });
    }

    @Test void existingConstructorStillPersistsANonNullVoucherName() {
        transaction.executeWithoutResult(status -> {
            entityManager.persist(new Voucher("VC1", "FPT", "FIXED", new BigDecimal("10000"),
                10, 0, 0, true));
            entityManager.flush();
            entityManager.clear();
            assertThat(entityManager.find(Voucher.class, "VC1").getVoucherName()).isEqualTo("FPT");
        });
    }

    @Test void refreshingAfterNativeReservationReadsCountersAndSqlVersionWithoutJpaIncrement() {
        jdbc.update("""
            INSERT INTO dbo.Voucher
              (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit)
            VALUES ('VC1', 'FPT', N'FPT discount', 'PERCENTAGE', 10, 5)
            """);

        transaction.executeWithoutResult(status -> {
            Voucher loaded = entityManager.find(Voucher.class, "VC1");
            assertThat(loaded.getVersion()).isZero();
            assertThat(vouchers.reserveOne("VC1")).isEqualTo(1);
            // Native SQL does not update already-managed objects; callers must refresh/reload.
            entityManager.refresh(loaded);
            assertThat(loaded.getVersion()).isEqualTo(1);
            assertThat(loaded.getReservedCount()).isEqualTo(1);
            assertThat(loaded.getRemainingCount()).isEqualTo(4);
            entityManager.flush();
        });

        assertThat(jdbc.queryForObject("SELECT version FROM dbo.Voucher WHERE voucher_id='VC1'", Long.class))
            .isEqualTo(1L);
    }
}
