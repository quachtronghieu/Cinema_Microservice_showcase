package vn.edu.fpt.cinema.catalog.movie;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.catalog.config.SecurityConfiguration;
import vn.edu.fpt.cinema.catalog.movie.controller.CustomerWatchlistRestController;
import vn.edu.fpt.cinema.catalog.movie.service.CustomerWatchlistService;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=CustomerWatchlistRestController.class, properties={
    "catalog.security.issuer=https://issuer.test", "catalog.security.jwk-set-uri=https://issuer.test/jwks",
    "catalog.security.audience=cinema-api"})
@Import({SecurityConfiguration.class, MovieWebTest.Keys.class})
class WatchlistWebTest {
    static final String URL="/api/v1/customer/watchlist";
    @Autowired MockMvc mvc;
    @MockitoBean CustomerWatchlistService service;

    static String customerToken(String customerId) throws Exception {
        var claims=new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString()).issuer("https://issuer.test")
            .audience("cinema-api").issueTime(Date.from(Instant.now())).notBeforeTime(Date.from(Instant.now().minusSeconds(1)))
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("roles",List.of("USER")).claim("customerId",customerId).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey)MovieWebTest.PAIR.getPrivate()));
        return "Bearer "+jwt.serialize();
    }
    @Test void anonymousCallerIsRejectedEvenWithAUserIdHeader() throws Exception {
        mvc.perform(get(URL).header("X-User-Id","victim")).andExpect(status().isUnauthorized());
        mvc.perform(put(URL+"/MV1").header("X-User-Id","victim")).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL+"/MV1").header("X-User-Id","victim")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void spoofedHeaderCannotReachAnotherCustomersWatchlist() throws Exception {
        var auth=customerToken("me");
        when(service.add("me","MV1")).thenReturn(true);
        mvc.perform(get(URL).header("Authorization",auth).header("X-User-Id","victim")).andExpect(status().isOk());
        mvc.perform(put(URL+"/MV1").header("Authorization",auth).header("X-User-Id","victim")).andExpect(status().isNoContent());
        mvc.perform(delete(URL+"/MV1").header("Authorization",auth).header("X-User-Id","victim")).andExpect(status().isNoContent());
        verify(service).list("me");
        verify(service).add("me","MV1");
        verify(service).remove("me","MV1");
        verifyNoMoreInteractions(service);
    }
    @Test void tokenWithoutCustomerIdIsForbidden() throws Exception {
        mvc.perform(get(URL).header("Authorization",MovieWebTest.token("ADMIN")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }
    @Test void unknownMovieIs404WithCommonError() throws Exception {
        mvc.perform(put(URL+"/missing").header("Authorization",customerToken("me")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
