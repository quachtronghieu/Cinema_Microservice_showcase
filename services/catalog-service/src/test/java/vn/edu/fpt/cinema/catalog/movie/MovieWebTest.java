package vn.edu.fpt.cinema.catalog.movie;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.edu.fpt.cinema.catalog.config.SecurityConfiguration;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.controller.*;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.entity.*;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import java.security.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={AdminMovieController.class, MovieController.class, MovieImageController.class}, properties={
    "catalog.security.issuer=https://issuer.test", "catalog.security.jwk-set-uri=https://issuer.test/jwks",
    "catalog.security.audience=cinema-api"})
@Import({SecurityConfiguration.class, MovieWebTest.Keys.class})
class MovieWebTest {
    static final String ADMIN="/api/v1/admin/movies";
    static final String PUBLIC="/api/v1/movies";
    static final KeyPair PAIR=keyPair();
    static final byte[] PNG={(byte)0x89,'P','N','G',0x0D,0x0A,0x1A,0x0A,0,0,0,0};
    @Autowired MockMvc mvc;
    @MockitoBean MovieService service;

    @TestConfiguration static class Keys {
        @Bean @Primary JwtDecoder testJwtDecoder() {
            var decoder=NimbusJwtDecoder.withPublicKey((RSAPublicKey)PAIR.getPublic()).build();
            decoder.setJwtValidator(SecurityConfiguration.validators("https://issuer.test","cinema-api"));
            return decoder;
        }
    }
    static KeyPair keyPair() {
        try { var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    static String token(String role) throws Exception {
        var claims=new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString()).issuer("https://issuer.test")
            .audience("cinema-api").issueTime(Date.from(Instant.now())).notBeforeTime(Date.from(Instant.now().minusSeconds(1)))
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("roles",List.of(role)).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey)PAIR.getPrivate()));
        return "Bearer "+jwt.serialize();
    }
    static MovieResponse response() {
        return new MovieResponse("MV000001","DUNE","Sci-Fi","A valid movie summary.",120,LocalDate.of(2026,12,1),"T13",
            "/assets/img/movies/uploads/id.png","https://youtu.be/abcdefghijk",true,0);
    }
    static MockMultipartFile image() { return new MockMultipartFile("image","poster.png","image/png",PNG); }
    static MockMultipartHttpServletRequestBuilder form(HttpMethod method, String uri) {
        var builder=multipart(method,uri);
        builder.param("title","Dune").param("genre","Sci-Fi").param("summary","A valid movie summary.")
            .param("duration","120").param("releaseDate","2026-12-01").param("ageRating","T13")
            .param("trailer","https://youtu.be/abcdefghijk");
        return builder;
    }

    @Test void guestsCanListAndReadWithoutToken() throws Exception {
        when(service.listPublic(MovieStatus.COMING_SOON,"dune",1,5)).thenReturn(new MoviePage(List.of(response()),1,5,6,2));
        when(service.detail("MV000001",true)).thenReturn(response());
        mvc.perform(get(PUBLIC+"?status=coming-soon&search=dune&page=1&size=5")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].movieId").value("MV000001")).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(PUBLIC+"/MV000001")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("DUNE"))
            .andExpect(jsonPath("$.releaseDate").value("2026-12-01"));
    }
    @Test void statusDefaultsToNowShowing() throws Exception {
        when(service.listPublic(MovieStatus.NOW_SHOWING,null,0,20)).thenReturn(new MoviePage(List.of(),0,20,0,0));
        mvc.perform(get(PUBLIC)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        verify(service).listPublic(MovieStatus.NOW_SHOWING,null,0,20);
    }
    @Test void unknownStatusIs400() throws Exception {
        mvc.perform(get(PUBLIC+"?status=archived")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }
    @Test void customerCannotOverrideActive() throws Exception {
        mvc.perform(get(PUBLIC+"?active=false")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void writesAreNotPublic() throws Exception {
        mvc.perform(post(PUBLIC)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(delete(PUBLIC+"/MV000001")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void missingAdminTokenIs401WithCommonError() throws Exception {
        mvc.perform(get(ADMIN)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors").isMap());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"USER","CASHIER_STAFF"})
    void nonAdminCannotAccessAnyAdminCommand(String role) throws Exception {
        String auth=token(role);
        mvc.perform(get(ADMIN).header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN+"/MV000001").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(form(HttpMethod.POST,ADMIN).file(image()).header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(form(HttpMethod.PUT,ADMIN+"/MV000001").param("version","0").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(delete(ADMIN+"/MV000001?version=0").header("Authorization",auth)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminCreatesWithMultipartAndReceivesLocation() throws Exception {
        when(service.create(any(),any())).thenReturn(response());
        mvc.perform(form(HttpMethod.POST,ADMIN).file(image()).header("Authorization",token("ADMIN")))
            .andExpect(status().isCreated()).andExpect(header().string("Location",ADMIN+"/MV000001"))
            .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.active").value(true));
        verify(service).create(argThat(request -> request.title().equals("Dune") && request.duration()==120
            && request.releaseDate().equals(LocalDate.of(2026,12,1))),argThat(file -> file.getSize()==PNG.length));
    }
    @Test void jsonBodyIsNotAccepted() throws Exception {
        mvc.perform(post(ADMIN).header("Authorization",token("ADMIN")).contentType("application/json").content("{}"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        verifyNoInteractions(service);
    }
    @ParameterizedTest @CsvSource({
        "title,A", "title,Tên Phim", "genre,'Sci<Fi'", "summary,short", "duration,60", "duration,181", "duration,abc",
        "releaseDate,01-12-2026", "ageRating,X", "trailer,https://evil.test/video"})
    void invalidFormFieldsHaveFieldErrors(String field, String value) throws Exception {
        var builder=multipart(HttpMethod.POST,ADMIN);
        Map.of("title","Dune","genre","Sci-Fi","summary","A valid movie summary.","duration","120",
               "releaseDate","2026-12-01","ageRating","T13","trailer","https://youtu.be/abcdefghijk")
            .forEach((key,fallback) -> builder.param(key,key.equals(field)?value:fallback));
        mvc.perform(builder.file(image()).header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors."+field).isArray());
        verifyNoInteractions(service);
    }
    @Test void bindingFailureDoesNotExposeJavaDetails() throws Exception {
        var builder=multipart(HttpMethod.POST,ADMIN);
        Map.of("title","Dune","genre","Sci-Fi","summary","A valid movie summary.","duration","abc",
               "releaseDate","2026-12-01","ageRating","T13","trailer","https://youtu.be/abcdefghijk")
            .forEach(builder::param);
        mvc.perform(builder.file(image()).header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.duration[0]").value("Invalid value"));
    }
    @ParameterizedTest @ValueSource(strings={"active","movieId","img","unknown"})
    void serverOwnedOrUnknownFieldsAreRejected(String field) throws Exception {
        mvc.perform(form(HttpMethod.POST,ADMIN).file(image()).param(field,"x").header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors."+field).isArray());
        verifyNoInteractions(service);
    }
    @Test void updateRequiresVersion() throws Exception {
        mvc.perform(form(HttpMethod.PUT,ADMIN+"/MV000001").header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.version").isArray());
        verifyNoInteractions(service);
    }
    @Test void updateAllowsOmittedImage() throws Exception {
        when(service.update(eq("MV000001"),any(),isNull())).thenReturn(response());
        mvc.perform(form(HttpMethod.PUT,ADMIN+"/MV000001").param("version","3").header("Authorization",token("ADMIN")))
            .andExpect(status().isOk());
        verify(service).update(eq("MV000001"),argThat(request -> request.version()==3),isNull());
    }
    @Test void conflictUsesCommonError() throws Exception {
        when(service.update(anyString(),any(),any())).thenThrow(new CatalogException(HttpStatus.CONFLICT,"MOVIE_VERSION_CONFLICT","Movie changed"));
        mvc.perform(form(HttpMethod.PUT,ADMIN+"/MV000001").param("version","0").header("Authorization",token("ADMIN")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MOVIE_VERSION_CONFLICT"))
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }
    @Test void serviceFieldErrorsReachTheClient() throws Exception {
        when(service.create(any(),any())).thenThrow(new CatalogException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Invalid movie data",
            Map.of("releaseDate",List.of("Release date must be at least 2 weeks after today"))));
        mvc.perform(form(HttpMethod.POST,ADMIN).file(image()).header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.releaseDate[0]").isNotEmpty());
    }
    @Test void oversizedUploadIs413() throws Exception {
        when(service.create(any(),any())).thenThrow(new MaxUploadSizeExceededException(5));
        mvc.perform(form(HttpMethod.POST,ADMIN).file(image()).header("Authorization",token("ADMIN")))
            .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("IMAGE_TOO_LARGE"));
    }
    @ParameterizedTest @ValueSource(strings={"-1","1.5","wrong"})
    void malformedDeleteVersionIs400(String version) throws Exception {
        mvc.perform(delete(ADMIN+"/MV000001?version="+version).header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void deleteReturns204WithoutBody() throws Exception {
        mvc.perform(delete(ADMIN+"/MV000001?version=0").header("Authorization",token("ADMIN")))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).deactivate("MV000001",0);
    }
    @Test void adminFiltersHaveStrictBooleanSemantics() throws Exception {
        when(service.listAdmin("x",false,0,20)).thenReturn(new MoviePage(List.of(),0,20,0,0));
        mvc.perform(get(ADMIN+"?search=x&active=false").header("Authorization",token("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get(ADMIN+"?active=yes").header("Authorization",token("ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void uploadedImageIsPublicCacheableAndTyped() throws Exception {
        when(service.image("id")).thenReturn(new MovieImage("id","image/png",PNG));
        mvc.perform(get("/assets/img/movies/uploads/id.png")).andExpect(status().isOk())
            .andExpect(content().contentType("image/png")).andExpect(content().bytes(PNG))
            .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("immutable")))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void wrongImageExtensionIs404() throws Exception {
        when(service.image("id")).thenReturn(new MovieImage("id","image/png",PNG));
        mvc.perform(get("/assets/img/movies/uploads/id.jpg")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
    }
    @Test void missingLegacyPosterIs404NotServerError() throws Exception {
        mvc.perform(get("/assets/img/movies/does-not-exist.jpg")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test void bundledLegacyPosterIsServed() throws Exception {
        mvc.perform(get("/assets/img/movies/THE_ODYSSEY.jpg")).andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"));
    }
}
