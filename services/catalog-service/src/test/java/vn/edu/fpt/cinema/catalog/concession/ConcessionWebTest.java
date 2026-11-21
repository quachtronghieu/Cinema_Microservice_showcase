package vn.edu.fpt.cinema.catalog.concession;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.catalog.config.SecurityConfiguration;
import vn.edu.fpt.cinema.catalog.concession.controller.*;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.ConcessionType;
import vn.edu.fpt.cinema.catalog.concession.service.ConcessionService;
import vn.edu.fpt.cinema.catalog.exception.*;
import java.math.BigDecimal;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={AdminConcessionController.class, CustomerConcessionController.class}, properties={
    "catalog.security.issuer=https://issuer.test", "catalog.security.jwk-set-uri=https://issuer.test/jwks",
    "catalog.security.audience=cinema-api"})
@Import({SecurityConfiguration.class, ConcessionWebTest.Keys.class})
class ConcessionWebTest {
    static final String ADMIN="/api/v1/admin/concessions";
    static final String CUSTOMER="/api/v1/concessions";
    static final String BODY="{\"type\":\"PC\",\"name\":\"Popcorn\",\"description\":\"Sweet corn.\",\"price\":50000,\"img\":\"popcorn.png\"}";
    static final KeyPair PAIR=keyPair();
    @Autowired MockMvc mvc;
    @MockitoBean ConcessionService service;

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
    static String token(String role, String issuer, String audience, Instant expiration, KeyPair pair) throws Exception {
        var claims=new JWTClaimsSet.Builder().subject(UUID.randomUUID().toString()).issuer(issuer)
            .audience(audience).issueTime(Date.from(Instant.now())).notBeforeTime(Date.from(Instant.now().minusSeconds(1)))
            .expirationTime(Date.from(expiration)).claim("roles",List.of(role)).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey)pair.getPrivate()));
        return "Bearer "+jwt.serialize();
    }
    static String token(String role) throws Exception {
        return token(role,"https://issuer.test","cinema-api",Instant.now().plusSeconds(300),PAIR);
    }
    static ConcessionResponse response() {
        return new ConcessionResponse("PC000001",ConcessionType.PC,"Popcorn","Sweet corn.",new BigDecimal("50000.00"),
            "/assets/img/concessions/popcorn.png",true,0);
    }
    @Test void missingTokenIs401WithCommonError() throws Exception {
        mvc.perform(get(ADMIN)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors").isMap());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"USER","CASHIER_STAFF","REDEMPTION_STAFF"})
    void nonAdminCannotAccessAnyAdminCommand(String role) throws Exception {
        String auth=token(role);
        mvc.perform(get(ADMIN).header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get(ADMIN+"/PC000001").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(post(ADMIN).header("Authorization",auth).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(ADMIN+"/PC000001").header("Authorization",auth).contentType("application/json")
            .content(BODY.replace("}",",\"version\":0}"))).andExpect(status().isForbidden());
        mvc.perform(delete(ADMIN+"/PC000001?version=0").header("Authorization",auth)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void signedAdminCanCreateAndReceiveLocation() throws Exception {
        when(service.create(any())).thenReturn(response());
        mvc.perform(post(ADMIN).header("Authorization",token("ADMIN")).contentType("application/json").content(BODY))
            .andExpect(status().isCreated()).andExpect(header().string("Location",ADMIN+"/PC000001"))
            .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.active").value(true));
    }
    @ParameterizedTest @ValueSource(strings={"25000.50","0.01"})
    void fractionalPricesAreRejectedForCreateAndUpdate(String price) throws Exception {
        var body=BODY.replace("50000",price);
        var auth=token("ADMIN");
        mvc.perform(post(ADMIN).header("Authorization",auth).contentType("application/json").content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors.price").isArray());
        mvc.perform(put(ADMIN+"/PC000001").header("Authorization",auth).contentType("application/json")
            .content(body.replace("}",",\"version\":0}")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.price").isArray());
        verifyNoInteractions(service);
    }
    @Test void wholeVndWithTrailingZerosIsAcceptedForCreateAndUpdate() throws Exception {
        var body=BODY.replace("50000","50000.00");
        var auth=token("ADMIN");
        when(service.create(any())).thenReturn(response());
        when(service.update(eq("PC000001"),any())).thenReturn(response());
        mvc.perform(post(ADMIN).header("Authorization",auth).contentType("application/json").content(body))
            .andExpect(status().isCreated());
        mvc.perform(put(ADMIN+"/PC000001").header("Authorization",auth).contentType("application/json")
            .content(body.replace("}",",\"version\":0}"))).andExpect(status().isOk());
        verify(service).create(argThat(request -> request.price().compareTo(new BigDecimal("50000"))==0
            && request.price().scale()<=0));
        verify(service).update(eq("PC000001"),argThat(request -> request.price().compareTo(new BigDecimal("50000"))==0
            && request.price().scale()<=0));
    }
    @Test void customerListAlwaysFiltersActive() throws Exception {
        when(service.list(ConcessionType.PC,true,0,20)).thenReturn(new ConcessionPage(List.of(response()),0,20,1,1));
        mvc.perform(get(CUSTOMER+"?type=PC").header("Authorization",token("USER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].active").value(true));
        verify(service).list(ConcessionType.PC,true,0,20);
    }
    @Test void customerCannotOverrideActive() throws Exception {
        mvc.perform(get(CUSTOMER+"?active=false").header("Authorization",token("USER")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"\"active\":false","\"version\":0","\"unknown\":1","\"concessionId\":\"mine\""})
    void createRejectsUnknownOrServerOwnedFields(String extra) throws Exception {
        mvc.perform(post(ADMIN).header("Authorization",token("ADMIN")).contentType("application/json")
            .content(BODY.replace("}",","+extra+"}"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void invalidImageHasFieldErrors() throws Exception {
        mvc.perform(post(ADMIN).header("Authorization",token("ADMIN")).contentType("application/json")
            .content(BODY.replace("popcorn.png","../popcorn.png"))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.img").isArray());
        verifyNoInteractions(service);
    }
    @Test void numericTypeIsRejected() throws Exception {
        mvc.perform(post(ADMIN).header("Authorization",token("ADMIN")).contentType("application/json")
            .content(BODY.replace("\"PC\"","1"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void adminFiltersHaveStrictBooleanSemantics() throws Exception {
        when(service.list(ConcessionType.PC,false,0,20)).thenReturn(new ConcessionPage(List.of(),0,20,0,0));
        mvc.perform(get(ADMIN+"?type=PC&active=false").header("Authorization",token("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get(ADMIN+"?active=yes").header("Authorization",token("ADMIN"))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"-1","1.5","wrong"})
    void malformedDeleteVersionIs400(String version) throws Exception {
        mvc.perform(delete(ADMIN+"/PC000001?version="+version).header("Authorization",token("ADMIN")))
            .andExpect(status().isBadRequest());
    }
    @Test void missingDeleteVersionIs400() throws Exception {
        mvc.perform(delete(ADMIN+"/PC000001").header("Authorization",token("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void fractionalBodyVersionIs400() throws Exception {
        mvc.perform(put(ADMIN+"/PC000001").header("Authorization",token("ADMIN")).contentType("application/json")
            .content(BODY.replace("}",",\"version\":0.5}"))).andExpect(status().isBadRequest());
    }
    @Test void conflictUsesCommonError() throws Exception {
        when(service.update(anyString(),any())).thenThrow(new CatalogException(org.springframework.http.HttpStatus.CONFLICT,
            "CONCESSION_VERSION_CONFLICT","Concession changed"));
        mvc.perform(put(ADMIN+"/PC000001").header("Authorization",token("ADMIN")).contentType("application/json")
            .content(BODY.replace("}",",\"version\":0}"))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONCESSION_VERSION_CONFLICT")).andExpect(jsonPath("$.fieldErrors").isMap());
    }
    @Test void deleteReturns204WithoutBody() throws Exception {
        mvc.perform(delete(ADMIN+"/PC000001?version=0").header("Authorization",token("ADMIN")))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
    }
    @Test void customerInactiveDetailIs404() throws Exception {
        when(service.detail("PC000001",true)).thenThrow(new CatalogException(org.springframework.http.HttpStatus.NOT_FOUND,
            "CONCESSION_NOT_FOUND","Concession not found"));
        mvc.perform(get(CUSTOMER+"/PC000001").header("Authorization",token("USER"))).andExpect(status().isNotFound());
    }
    @Test void realDecoderRejectsBadSignatureIssuerAudienceAndExpiredTokens() throws Exception {
        var tokens=List.of(
            token("ADMIN","https://wrong.test","cinema-api",Instant.now().plusSeconds(300),PAIR),
            token("ADMIN","https://issuer.test","wrong-api",Instant.now().plusSeconds(300),PAIR),
            token("ADMIN","https://issuer.test","cinema-api",Instant.now().minusSeconds(120),PAIR),
            token("ADMIN","https://issuer.test","cinema-api",Instant.now().plusSeconds(300),keyPair()));
        for (String auth:tokens) mvc.perform(get(ADMIN).header("Authorization",auth))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }
}
