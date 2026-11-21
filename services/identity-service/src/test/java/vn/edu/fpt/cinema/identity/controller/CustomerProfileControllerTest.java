package vn.edu.fpt.cinema.identity.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.identity.application.CustomerProfileService;
import vn.edu.fpt.cinema.identity.security.JwtTokenService;

@WebMvcTest(CustomerProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerProfileControllerTest {
    static final String URL = "/api/v1/customers/profile/me";

    @Autowired MockMvc mvc;
    @MockitoBean CustomerProfileService profiles;
    @MockitoBean JwtTokenService tokens;

    @Test void userIdHeaderAloneIsNotEnough() throws Exception {
        mvc.perform(get(URL).header("X-User-Id", "victim")).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).header("X-User-Id", "victim").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test void invalidTokenIsRejected() throws Exception {
        when(tokens.verifyToken("bad")).thenReturn(false);
        mvc.perform(get(URL).header("Authorization", "Bearer bad")).andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test void spoofedHeaderCannotChangeWhoseProfileIsUsed() throws Exception {
        when(tokens.verifyToken("good")).thenReturn(true);
        when(tokens.parseClaims("good")).thenReturn(Map.of("customerId", "me"));
        when(profiles.updateProfile(anyString(), any())).thenReturn(Map.of());

        mvc.perform(get(URL).header("Authorization", "Bearer good").header("X-User-Id", "victim"))
            .andExpect(status().isNotFound());
        mvc.perform(put(URL).header("Authorization", "Bearer good").header("X-User-Id", "victim")
                .contentType("application/json").content("{}"))
            .andExpect(status().isOk());

        verify(profiles).getProfileByUserId("me");
        verify(profiles).updateProfile(eq("me"), any());
        verifyNoMoreInteractions(profiles);
    }

    @Test void staffTokenWithoutCustomerIdHasNoProfile() throws Exception {
        when(tokens.verifyToken("staff")).thenReturn(true);
        when(tokens.parseClaims("staff")).thenReturn(Map.of("staffId", "ST1"));
        mvc.perform(get(URL).header("Authorization", "Bearer staff")).andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }
}
