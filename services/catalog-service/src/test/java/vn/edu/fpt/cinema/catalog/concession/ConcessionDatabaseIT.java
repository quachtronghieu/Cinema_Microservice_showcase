package vn.edu.fpt.cinema.catalog.concession;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.*;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.*;
import vn.edu.fpt.cinema.catalog.concession.service.ConcessionService;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "eureka.client.enabled=false",
    "catalog.security.issuer=https://issuer.test", "catalog.security.jwk-set-uri=https://issuer.test/jwks",
    "catalog.security.audience=cinema-api"})
@AutoConfigureMockMvc
@Import({ConcessionWebTest.Keys.class, ConcessionDatabaseIT.SqlObservation.class})
@Testcontainers(disabledWithoutDocker=true)
class ConcessionDatabaseIT {
    @Container static final MSSQLServerContainer<?> SQL =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04").acceptLicense();
    static final AtomicInteger UPDATES=new AtomicInteger();
    @TestConfiguration static class SqlObservation {
        @Bean HibernatePropertiesCustomizer statements() {
            return props -> props.put("hibernate.session_factory.statement_inspector",
                (org.hibernate.resource.jdbc.spi.StatementInspector) sql -> {
                    if (sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("update ")) UPDATES.incrementAndGet();
                    return sql;
                });
        }
    }
    static String databaseUrl(String database) {
        return "jdbc:sqlserver://"+SQL.getHost()+":"+SQL.getMappedPort(1433)+";databaseName="+database+";encrypt=false";
    }
    static void createDatabase(String database) {
        try (var c=DriverManager.getConnection(SQL.getJdbcUrl(),SQL.getUsername(),SQL.getPassword());
             var s=c.createStatement()) { s.execute("CREATE DATABASE ["+database+"]"); }
        catch (SQLException e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        createDatabase("catalog_db");
        registry.add("spring.datasource.url", () -> databaseUrl("catalog_db"));
        registry.add("spring.datasource.username", SQL::getUsername);
        registry.add("spring.datasource.password", SQL::getPassword);
        registry.add("spring.flyway.url", () -> databaseUrl("catalog_db"));
        registry.add("spring.flyway.user", SQL::getUsername);
        registry.add("spring.flyway.password", SQL::getPassword);
    }
    @Autowired ConcessionService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @BeforeEach void reset() { jdbc.update("DELETE FROM dbo.Concession"); UPDATES.set(0); }
    CreateConcessionRequest createRequest(ConcessionType type) {
        return new CreateConcessionRequest(type,"Popcorn","Sweet corn.",new BigDecimal("50000.00"),"popcorn.png");
    }
    UpdateConcessionRequest updateRequest(long version, String name) {
        return new UpdateConcessionRequest(ConcessionType.PC,name,"Sweet corn.",new BigDecimal("50000"),"popcorn.png",version);
    }
    @Test void cleanSchemaCreateUsesUuidAndBackendDefaults() {
        var item=service.create(createRequest(ConcessionType.PC));
        assertThat(UUID.fromString(item.concessionId()).toString()).isEqualTo(item.concessionId());
        assertThat(item.active()).isTrue();
        assertThat(item.version()).isZero();
        assertThat(jdbc.queryForObject("SELECT DB_NAME()",String.class)).isEqualTo("catalog_db");
    }
    @Test void identicalNormalizedPutDoesNotWriteOrIncrementAndStaleRequestStillConflicts() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        UPDATES.set(0);
        String body=ConcessionWebTest.BODY.replace("Popcorn","  Popcorn  ").replace("50000","50000.00").replace("}",",\"version\":0}");
        mvc.perform(put("/api/v1/admin/concessions/"+item.concessionId())
            .header("Authorization",ConcessionWebTest.token("ADMIN")).contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.active").value(true));
        assertThat(UPDATES).hasValue(0);
        assertThat(service.detail(item.concessionId(),false).version()).isZero();
        service.update(item.concessionId(),updateRequest(0,"New Popcorn"));
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
        assertThatThrownBy(() -> service.update(item.concessionId(),updateRequest(0,"New Popcorn")))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("CONCESSION_VERSION_CONFLICT"));
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
    }
    @Test void inactiveNoOpPutKeepsInactiveAndVersion() {
        var item=service.create(createRequest(ConcessionType.PC));
        service.deactivate(item.concessionId(),0);
        UPDATES.set(0);
        var result=service.update(item.concessionId(),updateRequest(1,"Popcorn"));
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.active()).isFalse();
        assertThat(UPDATES).hasValue(0);
    }
    @Test void repeatedDeleteWithOldVersionDoesNotWriteAgain() {
        var item=service.create(createRequest(ConcessionType.PC));
        service.deactivate(item.concessionId(),0);
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
        UPDATES.set(0);
        service.deactivate(item.concessionId(),0);
        assertThat(UPDATES).hasValue(0);
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
    }
    @Test void filtersActiveAndTypeAndKeepsAdminAccess() {
        var pc=service.create(createRequest(ConcessionType.PC));
        service.create(createRequest(ConcessionType.DR));
        service.deactivate(pc.concessionId(),0);
        assertThat(service.list(null,true,0,20).items()).hasSize(1).allMatch(ConcessionResponse::active);
        assertThat(service.list(ConcessionType.PC,true,0,20).items()).isEmpty();
        assertThat(service.list(ConcessionType.PC,null,0,20).items()).hasSize(1);
        assertThat(service.detail(pc.concessionId(),false).active()).isFalse();
        assertThatThrownBy(() -> service.detail(pc.concessionId(),true)).isInstanceOf(CatalogException.class);
    }
    @Test void customerHttpPaginationReturnsEveryActiveUuidAndHidesInactiveDetail() throws Exception {
        var expected=new ArrayList<String>();
        for (int i=0; i<21; i++) {
            expected.add(service.create(createRequest(i==20 ? ConcessionType.DR : ConcessionType.PC)).concessionId());
        }
        Collections.sort(expected);
        var inactive=service.create(createRequest(ConcessionType.DR));
        service.deactivate(inactive.concessionId(),0);
        var auth=ConcessionWebTest.token("USER");
        var received=new ArrayList<String>();
        for (int page=0; page<2; page++) {
            var result=mvc.perform(get("/api/v1/concessions?page="+page+"&size=20")
                    .header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(page==0 ? 20 : 1)).andReturn();
            for (var item : json.readTree(result.getResponse().getContentAsString()).path("items")) {
                assertThat(item.path("active").asBoolean()).isTrue();
                received.add(item.path("concessionId").asText());
            }
        }
        assertThat(received).containsExactlyElementsOf(expected).doesNotContain(inactive.concessionId());
        mvc.perform(get("/api/v1/concessions?page=2&size=20").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/v1/concessions/"+inactive.concessionId()).header("Authorization",auth))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CONCESSION_NOT_FOUND"));
        mvc.perform(get("/api/v1/admin/concessions/"+inactive.concessionId())
                .header("Authorization",ConcessionWebTest.token("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }
    @Test void concurrentChangedPutsHaveExactlyOneWinner() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        var results=race(() -> service.update(item.concessionId(),updateRequest(0,"New Popcorn")),
                         () -> service.update(item.concessionId(),updateRequest(0,"Other Popcorn")));
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
    }
    @Test void concurrentNoOpPutsBothSucceedWithoutWrite() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        UPDATES.set(0);
        assertThat(race(() -> service.update(item.concessionId(),updateRequest(0,"Popcorn")),
                        () -> service.update(item.concessionId(),updateRequest(0,"Popcorn"))))
            .containsExactly(200,200);
        assertThat(UPDATES).hasValue(0);
        assertThat(service.detail(item.concessionId(),false).version()).isZero();
    }
    @Test void putCompetingWithDeleteHasOneWinningWrite() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        var results=race(() -> service.update(item.concessionId(),updateRequest(0,"New Popcorn")),
                         () -> service.deactivate(item.concessionId(),0));
        assertThat(results).containsExactlyInAnyOrder(200,409);
        var stored=service.detail(item.concessionId(),false);
        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.active() ? stored.name() : "inactive").isIn("New Popcorn","inactive");
    }
    @Test void concurrentDeletesOnlyIncrementOnce() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        assertThat(race(() -> service.deactivate(item.concessionId(),0), () -> service.deactivate(item.concessionId(),0)))
            .containsExactly(200,200); // Service completion; HTTP DELETE maps success to 204.
        assertThat(service.detail(item.concessionId(),false).version()).isEqualTo(1);
        assertThat(service.detail(item.concessionId(),false).active()).isFalse();
    }
    @Test void noOpPutCompetingWithDeleteOnlyDeleteIncrementsVersion() throws Exception {
        var item=service.create(createRequest(ConcessionType.PC));
        UPDATES.set(0);
        var results=race(() -> service.update(item.concessionId(),updateRequest(0,"Popcorn")),
                         () -> service.deactivate(item.concessionId(),0));
        assertThat(results.get(0)).isIn(200,409);
        assertThat(results.get(1)).isEqualTo(200);
        assertThat(UPDATES).hasValue(1);
        var stored=service.detail(item.concessionId(),false);
        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.active()).isFalse();
    }
    @Test void concurrentCreatesHaveDistinctIds() throws Exception {
        assertThat(race(() -> service.create(createRequest(ConcessionType.PC)), () -> service.create(createRequest(ConcessionType.PC))))
            .containsExactly(200,200);
        assertThat(service.list(null,null,0,20).items()).extracting(ConcessionResponse::concessionId).doesNotHaveDuplicates().hasSize(2);
    }
    @Test void stagedMigrationPreservesIdsAndMoneyAndBackfillsFields() throws Exception {
        String name="migration_"+UUID.randomUUID().toString().replace("-","");
        createDatabase(name);
        var flyway=Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).target("1").load();
        flyway.migrate();
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement()) {
            s.execute("INSERT INTO dbo.Concession VALUES ('PC000001','Popcorn',50000.25,'Sweet corn.','/assets/img/concessions/popcorn.png')");
            s.execute("INSERT INTO dbo.Concession VALUES ('DR000001','Cola',15000,'Cold cola.','/assets/img/concessions/coca.png')");
        }
        Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).load().migrate();
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement();
             var rows=s.executeQuery("SELECT concession_id,type,active,version,price FROM dbo.Concession ORDER BY concession_id")) {
            assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("DR000001");
            assertThat(rows.getString(2)).isEqualTo("DR"); assertThat(rows.getBoolean(3)).isTrue(); assertThat(rows.getLong(4)).isZero();
            assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("PC000001");
            assertThat(rows.getString(2)).isEqualTo("PC"); assertThat(rows.getBigDecimal(5)).isEqualByComparingTo("50000.25");
            assertThat(rows.next()).isFalse();
        }
    }
    @ParameterizedTest @CsvSource({"XX000001,1", "PC000001,0"})
    void invalidLegacyDataBlocksMigrationWithoutPartialSchemaChanges(String id, int price) throws Exception {
        String name="migration_"+UUID.randomUUID().toString().replace("-","");
        createDatabase(name);
        Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).target("1").load().migrate();
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement()) {
            s.execute("INSERT INTO dbo.Concession VALUES ('"+id+"','Unknown',"+price+",'Unknown item.','/assets/img/concessions/popcorn.png')");
        }
        assertThatThrownBy(() -> Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).load().migrate())
            .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement();
             var rows=s.executeQuery("SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.Concession') AND name='active'")) {
            rows.next(); assertThat(rows.getInt(1)).isZero();
        }
    }
    private List<Integer> race(Runnable first, Runnable second) throws Exception {
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try (var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> a=() -> outcome(first,ready,start);
            Callable<Integer> b=() -> outcome(second,ready,start);
            var one=executor.submit(a); var two=executor.submit(b);
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(30,TimeUnit.SECONDS),two.get(30,TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }
    private int outcome(Runnable action, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test did not start");
        try { action.run(); return 200; }
        catch (CatalogException e) { return e.status().value(); }
    }
}
