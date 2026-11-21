package vn.edu.fpt.cinema.booking.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 * Real SQL Server 2022 with Flyway V1 applied; seat/voucher invariants need real locks and constraints, not mocks.
 * Uses the git-ignored repo-root booking-it.properties (url, username, password of a test-only database) when present,
 * otherwise a Testcontainers SQL Server. Skipped when neither is available.
 */
@SpringBootTest(properties = {
    "eureka.client.enabled=false", "booking.seat-hold.expiry-delay=PT1H",
    "booking.security.issuer=" + TestJwt.ISSUER, "booking.security.jwk-set-uri=https://issuer.test/jwks",
    "booking.security.audience=" + TestJwt.AUDIENCE})
@Import(TestJwt.Keys.class)
@EnabledIf("vn.edu.fpt.cinema.booking.support.SqlServerIT#databaseAvailable")
public abstract class SqlServerIT {
    private static final Properties LOCAL = localDatabase();
    private static MSSQLServerContainer<?> container;

    static boolean databaseAvailable() {
        return LOCAL != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url;
        String username;
        String password;
        if (LOCAL != null) {
            url = LOCAL.getProperty("url");
            username = LOCAL.getProperty("username");
            password = LOCAL.getProperty("password");
        } else {
            var sql = container();
            url = "jdbc:sqlserver://" + sql.getHost() + ":" + sql.getMappedPort(1433) + ";databaseName=booking_db;encrypt=false";
            username = sql.getUsername();
            password = sql.getPassword();
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.flyway.url", () -> url);
        registry.add("spring.flyway.user", () -> username);
        registry.add("spring.flyway.password", () -> password);
    }

    private static synchronized MSSQLServerContainer<?> container() {
        if (container == null) {
            container = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04").acceptLicense();
            container.start();
            try (var c = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
                 var s = c.createStatement()) {
                s.execute("IF DB_ID('booking_db') IS NULL CREATE DATABASE booking_db");
            } catch (SQLException e) { throw new IllegalStateException(e); }
        }
        return container;
    }

    private static Properties localDatabase() {
        for (Path path : new Path[] {Path.of("booking-it.properties"), Path.of("../../booking-it.properties")}) {
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    var properties = new Properties();
                    properties.load(in);
                    return properties;
                } catch (IOException e) { throw new IllegalStateException("Cannot read " + path, e); }
            }
        }
        return null;
    }
}
