package vn.edu.fpt.cinema.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.fpt.cinema.booking.support.DbReset;
import vn.edu.fpt.cinema.booking.support.SqlServerIT;
import vn.edu.fpt.cinema.booking.support.TestJwt;

@AutoConfigureMockMvc
class AdminVoucherHttpIT extends SqlServerIT {
    static final String URL = "/api/v1/admin/vouchers";
    static final String CREATE = """
        {"code":" hieu10 ","voucherName":" Hieu discount ",
         "discountType":"PERCENTAGE","discountValue":10,"usageLimit":100}
        """;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void reset() { DbReset.clean(jdbc); }

    private void seed(int used, int reserved) {
        jdbc.update("""
            INSERT INTO dbo.Voucher
              (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count)
            VALUES ('VC1','HIEU10',N'Hieu discount','PERCENTAGE',10,100,?,?)
            """, used, reserved);
    }

    private ResultActions call(String method, String path, String body) throws Exception {
        var request = request(HttpMethod.valueOf(method), path).header("Authorization", TestJwt.admin("ST1"));
        if (body != null) request.contentType("application/json").content(body);
        return mvc.perform(request);
    }

    private void error(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status)).andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @ParameterizedTest
    @CsvSource({"GET,''", "GET,/VC1", "POST,''", "PATCH,/VC1", "POST,/VC1/disable"})
    void everyOperationRejectsMissingToken(String method, String suffix) throws Exception {
        error(mvc.perform(request(HttpMethod.valueOf(method), URL + suffix).contentType("application/json").content(CREATE)),
            401, "UNAUTHORIZED");
    }

    @ParameterizedTest
    @CsvSource({"GET,''", "GET,/VC1", "POST,''", "PATCH,/VC1", "POST,/VC1/disable"})
    void everyOperationRejectsUserToken(String method, String suffix) throws Exception {
        error(mvc.perform(request(HttpMethod.valueOf(method), URL + suffix).header("Authorization", TestJwt.customer("CS1"))
            .contentType("application/json").content(CREATE)), 403, "FORBIDDEN");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Voucher", Integer.class)).isZero();
    }

    @Test void invalidBearerUsesCommon401Error() throws Exception {
        error(mvc.perform(get(URL).header("Authorization", "Bearer invalid-fixture")), 401, "UNAUTHORIZED");
    }

    @Test void createThenReadThroughRealControllerAndDatabase() throws Exception {
        String location = call("POST", URL, CREATE).andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("HIEU10"))
            .andExpect(jsonPath("$.voucherName").value("Hieu discount"))
            .andExpect(jsonPath("$.status").value(true)).andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.usedCount").value(0)).andExpect(jsonPath("$.reservedCount").value(0))
            .andExpect(jsonPath("$.remainingCount").value(100))
            .andReturn().getResponse().getHeader("Location");
        assertThat(location).startsWith(URL + "/");
        call("GET", location, null).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("HIEU10"));
    }

    @Test void listFilterAndDefaultsIncludeDisabledAndExhausted() throws Exception {
        seed(100, 0);
        call("POST", URL + "/VC1/disable", null).andExpect(status().isNoContent());
        call("GET", URL, null).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].status").value(false))
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1));
        call("GET", URL + "?status=true", null).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        call("GET", URL + "?status=false", null).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?page=-1", "?size=0", "?size=101", "?page=1.5", "?size=bad", "?status=yes"})
    void invalidListParametersAre400(String query) throws Exception {
        error(call("GET", URL + query, null), 400, "VALIDATION_ERROR");
    }

    @Test void createMissingRequiredFieldReturnsFieldErrors() throws Exception {
        var result = call("POST", URL, CREATE.replace("\"usageLimit\":100", "\"usageLimit\":null"));
        error(result, 400, "VALIDATION_ERROR");
        result.andExpect(jsonPath("$.fieldErrors.usageLimit").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"status\":null}", "{\"version\":0}", "{\"description\":\"extra\"}",
        "{\"usageLimit\":\"100\"}", "{\"usageLimit\":1.5}", "{\"code\":123}",
        "{\"discountValue\":0}", "{\"voucherName\":\" \"}"
    })
    void patchInputErrorsAre400WithoutChangingStoredState(String body) throws Exception {
        seed(0, 0);
        var before = jdbc.queryForMap("SELECT * FROM dbo.Voucher");
        error(call("PATCH", URL + "/VC1", body), 400, "VALIDATION_ERROR");
        assertThat(jdbc.queryForMap("SELECT * FROM dbo.Voucher")).isEqualTo(before);
    }

    @Test void mergedDiscountValidationReturns400WithFieldErrors() throws Exception {
        seed(0, 0);
        var result = call("PATCH", URL + "/VC1", "{\"discountValue\":101}");
        error(result, 400, "VALIDATION_ERROR");
        result.andExpect(jsonPath("$.fieldErrors.discountValue").isArray());
    }

    @Test void protectedEditAndLimitConflictAre409WithRollback() throws Exception {
        seed(70, 20);
        var before = jdbc.queryForMap("SELECT * FROM dbo.Voucher");
        error(call("PATCH", URL + "/VC1", "{\"discountValue\":20}"), 409, "VOUCHER_EDIT_RESTRICTED");
        error(call("PATCH", URL + "/VC1", "{\"usageLimit\":89,\"status\":false}"), 409, "VOUCHER_LIMIT_CONFLICT");
        assertThat(jdbc.queryForMap("SELECT * FROM dbo.Voucher")).isEqualTo(before);
    }

    @Test void duplicateCodeReturns409WithoutSqlDetails() throws Exception {
        seed(0, 0);
        error(call("POST", URL, CREATE), 409, "VOUCHER_CODE_EXISTS");
    }

    @Test void patchChangeNoOpDisableRepeatAndEnableUseTheirContractVersions() throws Exception {
        seed(0, 1);
        call("PATCH", URL + "/VC1", "{\"usageLimit\":120}").andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.reservedCount").value(1));
        call("PATCH", URL + "/VC1", "{\"usageLimit\":120}").andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));
        for (int i = 0; i < 2; i++) call("POST", URL + "/VC1/disable", null)
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        call("GET", URL + "/VC1", null).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(false)).andExpect(jsonPath("$.version").value(2));
        call("PATCH", URL + "/VC1", "{\"status\":true}").andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.reservedCount").value(1));
    }

    @Test void disableRejectsBodyOrVersionQuery() throws Exception {
        seed(0, 0);
        error(call("POST", URL + "/VC1/disable", "{}"), 400, "VALIDATION_ERROR");
        error(call("POST", URL + "/VC1/disable?version=0", null), 400, "VALIDATION_ERROR");
        assertThat(jdbc.queryForObject("SELECT version FROM dbo.Voucher", Long.class)).isZero();
    }

    @Test void missingIdsAre404ForAllIdOperations() throws Exception {
        error(call("GET", URL + "/old-id", null), 404, "VOUCHER_NOT_FOUND");
        error(call("PATCH", URL + "/old-id", "{\"usageLimit\":10}"), 404, "VOUCHER_NOT_FOUND");
        error(call("POST", URL + "/old-id/disable", null), 404, "VOUCHER_NOT_FOUND");
    }

    @Test void noPhysicalDeleteOrFullPutEndpoint() throws Exception {
        seed(0, 0);
        error(call("DELETE", URL + "/VC1", null), 405, "METHOD_NOT_ALLOWED");
        error(call("PUT", URL + "/VC1", CREATE), 405, "METHOD_NOT_ALLOWED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Voucher", Integer.class)).isEqualTo(1);
    }
}
