package vn.edu.fpt.cinema.cinema.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.cinema.cinema.config.SecurityConfiguration;
import vn.edu.fpt.cinema.cinema.dto.RoomResponse;
import vn.edu.fpt.cinema.cinema.service.RoomService;
import vn.edu.fpt.cinema.cinema.support.TestJwt;

@WebMvcTest(controllers = RoomRestController.class, properties = {
    "cinema.security.issuer=" + TestJwt.ISSUER, "cinema.security.jwk-set-uri=https://issuer.test/jwks",
    "cinema.security.audience=" + TestJwt.AUDIENCE})
@Import({SecurityConfiguration.class, TestJwt.Keys.class})
class RoomWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean RoomService rooms;

    private static final String ROOM = "/api/v1/admin/rooms/1";

    @Test void noToken_401_userToken_403_withCommonErrorBody() throws Exception {
        mvc.perform(get("/api/v1/admin/rooms")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.fieldErrors").isMap());
        mvc.perform(get("/api/v1/admin/rooms").header("Authorization", TestJwt.customer("CS1")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(patch(ROOM + "/status").header("Authorization", TestJwt.customer("CS1"))
                .contentType("application/json").content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(rooms);
    }

    @Test void adminChangesStatus_actorIsStaffId_traceIdFromGateway() throws Exception {
        when(rooms.changeStatus(any(), any(), any(), any()))
            .thenReturn(new RoomResponse("1", "TM000001", "2D", "INACTIVE", 75, 21, 54));
        mvc.perform(patch(ROOM + "/status").header("Authorization", TestJwt.admin("ST000001"))
                .header("X-Trace-Id", "gw-trace-1").contentType("application/json").content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"))
            .andExpect(jsonPath("$.capacity").value(75));
        verify(rooms).changeStatus("1", "INACTIVE", "ST000001", "gw-trace-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":\"CLOSED\"}", "{\"status\":\"active\"}", "{\"status\":1}",
        "{\"status\":\"INACTIVE\",\"templateId\":\"TM000002\"}", "[]", "not json"})
    void invalidStatusBody_400(String body) throws Exception {
        mvc.perform(patch(ROOM + "/status").header("Authorization", TestJwt.admin("ST1"))
                .contentType("application/json").content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(rooms);
    }

    @ParameterizedTest
    @ValueSource(strings = {"post", "put", "delete"})
    void noRouteWritesRoomStructure(String method) throws Exception {
        var request = switch (method) {
            case "post" -> post("/api/v1/admin/rooms");
            case "put" -> put(ROOM);
            default -> delete(ROOM);
        };
        mvc.perform(request.header("Authorization", TestJwt.admin("ST1")).contentType("application/json").content("{}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(rooms);
    }

    @Test void internalSeatRoutesAreClosed() throws Exception {
        mvc.perform(post("/api/v1/cinema/internal/seats/lock").header("Authorization", TestJwt.admin("ST1")))
            .andExpect(status().isForbidden());
    }
}
