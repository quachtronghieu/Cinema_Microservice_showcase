package vn.edu.fpt.cinema.catalog.movie;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.*;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.entity.MovieStatus;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "eureka.client.enabled=false", "catalog.business-zone=Asia/Ho_Chi_Minh", "management.zipkin.tracing.export.enabled=false",
    "catalog.security.issuer=https://issuer.test", "catalog.security.jwk-set-uri=https://issuer.test/jwks",
    "catalog.security.audience=cinema-api"})
@AutoConfigureMockMvc
@AutoConfigureObservability(metrics=false)
@Import({MovieWebTest.Keys.class, MovieDatabaseIT.SqlObservation.class})
@Testcontainers(disabledWithoutDocker=true)
class MovieDatabaseIT {
    @Container static final MSSQLServerContainer<?> SQL =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04").acceptLicense();
    static final AtomicInteger UPDATES=new AtomicInteger();
    static final byte[] PNG={(byte)0x89,'P','N','G',0x0D,0x0A,0x1A,0x0A,1,2,3};
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
    @Autowired MovieService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    final LocalDate today=LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    @Autowired vn.edu.fpt.cinema.catalog.movie.service.CustomerWatchlistService watchlist;
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM dbo.Watchlist"); jdbc.update("DELETE FROM dbo.Movie"); jdbc.update("DELETE FROM dbo.Movie_Image");
        UPDATES.set(0);
    }

    MockMultipartFile png() { return new MockMultipartFile("image","poster.png","image/png",PNG); }
    MovieResponse create(String title) {
        return service.create(new CreateMovieRequest(title,"Sci-Fi","A valid movie summary.",120,today.plusMonths(1),"T13","https://youtu.be/abcdefghijk"),png());
    }
    UpdateMovieRequest updateRequest(long version, String title) {
        return new UpdateMovieRequest(title,"Sci-Fi","A valid movie summary.",120,today.plusMonths(1),"T13","https://youtu.be/abcdefghijk",version);
    }
    // Release windows forbid creating past movies through the API, so listings seed them directly.
    void insert(String id, String title, LocalDate date, boolean active) {
        jdbc.update("INSERT INTO dbo.Movie (movie_id,title,duration,genre,release_date,summary,age_rating,img,trailer,active,version) "
            +"VALUES (?,?,120,'Drama',?,'A valid movie summary.','P','/assets/img/movies/a.jpg','',?,0)",
            id,title,java.sql.Date.valueOf(date),active);
    }

    @Test void cleanSchemaCreateUsesUuidUppercaseTitleAndServesStoredImageOverHttp() throws Exception {
        var item=create("Dune Part Two");
        assertThat(UUID.fromString(item.movieId()).toString()).isEqualTo(item.movieId());
        assertThat(item.title()).isEqualTo("DUNE PART TWO");
        assertThat(item.active()).isTrue();
        assertThat(item.version()).isZero();
        assertThat(jdbc.queryForObject("SELECT DB_NAME()",String.class)).isEqualTo("catalog_db");
        mvc.perform(get(item.img())).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(PNG));
    }
    @Test void multipartCreateAndUpdateWorkThroughRealStack() throws Exception {
        var auth=MovieWebTest.token("ADMIN");
        var created=mvc.perform(multipart(HttpMethod.POST,"/api/v1/admin/movies").file(png()).header("Authorization",auth)
                .param("title","Tenet").param("genre","Sci-Fi").param("summary","A valid movie summary.").param("duration","150")
                .param("releaseDate",today.plusMonths(1).toString()).param("ageRating","T16").param("trailer","https://youtu.be/abcdefghijk"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("TENET")).andReturn();
        var body=json.readTree(created.getResponse().getContentAsString());
        String uri="/api/v1/admin/movies/"+body.path("movieId").asText();
        mvc.perform(multipart(HttpMethod.PUT,uri).header("Authorization",auth)
                .param("title","Tenet Two").param("genre","Sci-Fi").param("summary","A valid movie summary.").param("duration","150")
                .param("releaseDate",today.plusMonths(1).toString()).param("ageRating","T16").param("trailer","https://youtu.be/abcdefghijk")
                .param("version","0"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("TENET TWO")).andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.img").value(body.path("img").asText()));
    }
    @Test void publicListSplitsByReleaseWindowAndSortsAndHidesInactive() throws Exception {
        insert("A","TODAY MOVIE",today,true);
        insert("B","THIRTY DAYS AGO",today.minusDays(30),true);
        insert("C","TOO OLD",today.minusDays(31),true);
        insert("D","TOMORROW",today.plusDays(1),true);
        insert("E","NEXT MONTH",today.plusMonths(1),true);
        insert("F","HIDDEN NOW",today,false);
        insert("G","HIDDEN SOON",today.plusDays(2),false);
        assertThat(service.listPublic(MovieStatus.NOW_SHOWING,null,0,20).items()).extracting(MovieResponse::movieId).containsExactly("A","B");
        assertThat(service.listPublic(MovieStatus.COMING_SOON,null,0,20).items()).extracting(MovieResponse::movieId).containsExactly("D","E");
        assertThat(service.listAdmin(null,null,0,20).totalElements()).isEqualTo(7);
        assertThat(service.listAdmin(null,false,0,20).items()).extracting(MovieResponse::movieId).containsExactlyInAnyOrder("F","G");
        mvc.perform(get("/api/v1/movies?status=coming-soon")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/movies/F")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));
    }
    @Test void searchIsCaseInsensitiveWithinTabAndTreatsWildcardsLiterally() {
        insert("A","DUNE: PART TWO",today,true);
        insert("B","100% WOLF",today,true);
        insert("C","A_B [X]",today,true);
        insert("D","DUNE RETURNS",today.plusDays(3),true);
        assertThat(ids(MovieStatus.NOW_SHOWING,"dune")).containsExactly("A");
        assertThat(ids(MovieStatus.COMING_SOON,"  DuNe ")).containsExactly("D");
        assertThat(ids(MovieStatus.NOW_SHOWING,"%")).containsExactly("B");
        assertThat(ids(MovieStatus.NOW_SHOWING,"_")).containsExactly("C");
        assertThat(ids(MovieStatus.NOW_SHOWING,"[X]")).containsExactly("C");
        assertThat(ids(MovieStatus.NOW_SHOWING,"no such movie")).isEmpty();
        assertThat(ids(MovieStatus.NOW_SHOWING,"  ")).hasSize(3);
        assertThat(service.listAdmin("dune",null,0,20).totalElements()).isEqualTo(2);
    }
    List<String> ids(MovieStatus status, String search) {
        return service.listPublic(status,search,0,20).items().stream().map(MovieResponse::movieId).toList();
    }
    @Test void paginationWalksEveryRowOnceWithStableOrder() {
        for (int i=0; i<5; i++) insert("M"+i,"MOVIE "+i,today,true);
        var seen=new ArrayList<String>();
        for (int page=0; page<3; page++) seen.addAll(service.listPublic(MovieStatus.NOW_SHOWING,null,page,2).items().stream().map(MovieResponse::movieId).toList());
        assertThat(seen).containsExactly("M0","M1","M2","M3","M4");
    }
    @Test void identicalPutDoesNotWriteOrIncrementAndStaleRequestConflicts() {
        var item=create("Dune");
        UPDATES.set(0);
        var same=service.update(item.movieId(),updateRequest(0,"  dune "),null);
        assertThat(same.version()).isZero();
        assertThat(UPDATES).hasValue(0);
        service.update(item.movieId(),updateRequest(0,"New Dune"),null);
        assertThat(service.detail(item.movieId(),false).version()).isEqualTo(1);
        assertThatThrownBy(() -> service.update(item.movieId(),updateRequest(0,"New Dune"),null))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_VERSION_CONFLICT"));
    }
    @Test void replacingPosterKeepsPreviousImageServable() throws Exception {
        var item=create("Dune");
        var updated=service.update(item.movieId(),updateRequest(0,"Dune"),png());
        assertThat(updated.img()).isNotEqualTo(item.img());
        assertThat(updated.version()).isEqualTo(1);
        mvc.perform(get(item.img())).andExpect(status().isOk());
        mvc.perform(get(updated.img())).andExpect(status().isOk());
    }
    @Test void repeatedDeleteDoesNotWriteAgainAndTitleBecomesReusable() {
        var item=create("Dune");
        service.deactivate(item.movieId(),0);
        assertThat(service.detail(item.movieId(),false).version()).isEqualTo(1);
        UPDATES.set(0);
        service.deactivate(item.movieId(),0);
        assertThat(UPDATES).hasValue(0);
        var again=create("Dune");
        assertThat(again.movieId()).isNotEqualTo(item.movieId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Movie WHERE title='DUNE'",Integer.class)).isEqualTo(2);
    }
    @Test void duplicateActiveTitleConflictsCaseInsensitively() {
        create("Dune");
        assertThatThrownBy(() -> create("dUNE"))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_TITLE_CONFLICT"));
        var other=create("Arrival");
        assertThatThrownBy(() -> service.update(other.movieId(),updateRequest(0,"dune"),null))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_TITLE_CONFLICT"));
    }
    @Test void failedCreateLeavesNoImageBehind() {
        create("Dune");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Movie_Image",Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> create("Dune")).isInstanceOf(CatalogException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Movie_Image",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentCreatesOfSameTitleHaveExactlyOneWinnerAndNoStrayImage() throws Exception {
        for (int round=0; round<5; round++) {
            String title="Race "+round;
            assertThat(race(() -> create(title), () -> create(title))).containsExactlyInAnyOrder(200,409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Movie WHERE title=?",Integer.class,title.toUpperCase())).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Movie_Image",Integer.class)).isEqualTo(5);
    }
    @Test void concurrentChangedPutsHaveExactlyOneWinner() throws Exception {
        var item=create("Dune");
        var results=race(() -> service.update(item.movieId(),updateRequest(0,"New Dune"),null),
                         () -> service.update(item.movieId(),updateRequest(0,"Other Dune"),null));
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(service.detail(item.movieId(),false).version()).isEqualTo(1);
    }
    @Test void putCompetingWithDeleteHasOneWinningWrite() throws Exception {
        var item=create("Dune");
        var results=race(() -> service.update(item.movieId(),updateRequest(0,"New Dune"),null),
                         () -> service.deactivate(item.movieId(),0));
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(service.detail(item.movieId(),false).version()).isEqualTo(1);
    }
    @Test void concurrentDeletesOnlyIncrementOnce() throws Exception {
        var item=create("Dune");
        assertThat(race(() -> service.deactivate(item.movieId(),0), () -> service.deactivate(item.movieId(),0))).containsExactly(200,200);
        assertThat(service.detail(item.movieId(),false).version()).isEqualTo(1);
    }
    @Test void watchlistAcceptsUuidMoviesIsIdempotentAndHidesDeactivatedMovies() {
        var item=create("Dune");
        assertThat(watchlist.add("customer-1",item.movieId())).isTrue();
        assertThat(watchlist.add("customer-1",item.movieId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Watchlist",Integer.class)).isEqualTo(1);
        assertThat(watchlist.list("customer-1")).extracting(MovieResponse::movieId).containsExactly(item.movieId());
        assertThat(watchlist.list("customer-2")).isEmpty();
        assertThat(watchlist.add("customer-1","missing")).isFalse();
        service.deactivate(item.movieId(),0);
        assertThat(watchlist.list("customer-1")).isEmpty();
        assertThat(watchlist.add("customer-2",item.movieId())).isFalse();
        watchlist.remove("customer-1",item.movieId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Watchlist",Integer.class)).isZero();
    }
    @Test void inboundTraceparentBecomesTheTraceIdOfResponsesAndErrors() throws Exception {
        String traceId="4bf92f3577b34da6a3ce929d0e0e4736";
        String traceparent="00-"+traceId+"-00f067aa0ba902b7-01";
        mvc.perform(get("/api/v1/movies").header("traceparent",traceparent))
            .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id",traceId));
        mvc.perform(get("/api/v1/movies/missing").header("traceparent",traceparent))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.traceId").value(traceId));
    }
    @Test void stagedMigrationPreservesIdsAndBackfillsFields() throws Exception {
        String name=freshDatabaseAtV3();
        exec(name,"INSERT INTO dbo.Movie (movie_id,title,duration,genre,release_date,summary,age_rating,img,trailer) "
            +"VALUES ('MV000001','LEGACY MOVIE',120,'Drama','2026-01-02',NULL,NULL,'/assets/img/movies/gun.jpg','')");
        migrate(name);
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement();
             var rows=s.executeQuery("SELECT movie_id,active,version,summary,img FROM dbo.Movie")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("MV000001");
            assertThat(rows.getBoolean(2)).isTrue();
            assertThat(rows.getLong(3)).isZero();
            assertThat(rows.getString(4)).isNull();
            assertThat(rows.getString(5)).isEqualTo("/assets/img/movies/gun.jpg");
            assertThat(rows.next()).isFalse();
        }
    }
    @ParameterizedTest @ValueSource(strings={
        "('MV000001','SAME',120,'Drama','2026-01-02',NULL,NULL,'a',''),('MV000002','same',120,'Drama','2026-01-02',NULL,NULL,'a','')",
        "('MV000001','BAD DURATION',0,'Drama','2026-01-02',NULL,NULL,'a','')"})
    void invalidLegacyDataBlocksMigrationWithoutPartialSchemaChanges(String rows) throws Exception {
        String name=freshDatabaseAtV3();
        exec(name,"INSERT INTO dbo.Movie (movie_id,title,duration,genre,release_date,summary,age_rating,img,trailer) VALUES "+rows);
        assertThatThrownBy(() -> migrate(name)).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement();
             var result=s.executeQuery("SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.Movie') AND name='active'")) {
            result.next(); assertThat(result.getInt(1)).isZero();
        }
    }
    private String freshDatabaseAtV3() {
        String name="migration_"+UUID.randomUUID().toString().replace("-","");
        createDatabase(name);
        Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).target("3").load().migrate();
        return name;
    }
    private void migrate(String name) { Flyway.configure().dataSource(databaseUrl(name),SQL.getUsername(),SQL.getPassword()).load().migrate(); }
    private void exec(String name, String sql) throws SQLException {
        try (var c=DriverManager.getConnection(databaseUrl(name),SQL.getUsername(),SQL.getPassword()); var s=c.createStatement()) { s.execute(sql); }
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
