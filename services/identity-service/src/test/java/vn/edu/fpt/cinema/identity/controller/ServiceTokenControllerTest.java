package vn.edu.fpt.cinema.identity.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.identity.application.PasswordResetService;
import vn.edu.fpt.cinema.identity.application.ServiceClientService;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.security.JwtTokenService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceTokenControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ServiceClientService clients;
    @MockitoBean JwtTokenService tokens;
    @MockitoBean PrincipalRepository principals;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean PasswordResetService passwordResets;

    private ResultActions ask(String secret, String scope) throws Exception {
        return mvc.perform(post("/api/v1/auth/token/service").contentType("application/json").content("""
            {"clientId":"svc:cinema-service","clientSecret":"%s","targetService":"booking-service","scope":"%s"}
            """.formatted(secret, scope)));
    }

    @Test void wrongSecret_401_noToken() throws Exception {
        when(clients.authenticate("svc:cinema-service", "wrong")).thenReturn(false);
        ask("wrong", "sales:write").andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("SERVICE_CLIENT_INVALID"));
        verifyNoInteractions(tokens);
    }

    @Test void scopeNotGranted_403_noToken() throws Exception {
        when(clients.authenticate("svc:cinema-service", "right")).thenReturn(true);
        when(clients.hasGrant("svc:cinema-service", "booking-service", "admin:everything")).thenReturn(false);
        ask("right", "admin:everything").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SERVICE_SCOPE_NOT_GRANTED"));
        verifyNoInteractions(tokens);
    }

    @Test void validClientAndGrant_200_withToken() throws Exception {
        when(clients.authenticate("svc:cinema-service", "right")).thenReturn(true);
        when(clients.hasGrant("svc:cinema-service", "booking-service", "sales:write")).thenReturn(true);
        when(tokens.generateServiceToken("svc:cinema-service", "booking-service", "sales:write")).thenReturn("jwt");
        ask("right", "sales:write").andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("sales:write"));
        verify(tokens).generateServiceToken(eq("svc:cinema-service"), eq("booking-service"), eq("sales:write"));
    }
}
