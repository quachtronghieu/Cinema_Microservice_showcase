package vn.edu.fpt.cinema.booking.dto;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.fpt.cinema.booking.entity.Voucher;

/** Uses Boot's ObjectMapper with the production application.yaml coercion/unknown-field settings. */
@JsonTest
class VoucherDtoTest {
    private static final String CREATE = """
        {"code":"  hieu10  ","voucherName":"  Hieu discount  ",
         "discountType":"PERCENTAGE","discountValue":10.000,"usageLimit":100}
        """;
    static ValidatorFactory factory;
    static Validator validator;
    @Autowired ObjectMapper mapper;

    @BeforeAll static void validation() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll static void close() { factory.close(); }

    @Test void createNormalizesBeforeValidationUsingLocaleIndependentCode() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var request = mapper.readValue(CREATE, CreateVoucherRequest.class);
            assertThat(request.code()).isEqualTo("HIEU10");
            assertThat(request.voucherName()).isEqualTo("Hieu discount");
            assertThat(validator.validate(request)).isEmpty();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "100.01", "1.001", "100000000000000000"})
    void invalidPercentageAmountsHaveAFieldError(String amount) {
        var request = new CreateVoucherRequest("FPT", "Discount", VoucherDiscountType.PERCENTAGE,
            new BigDecimal(amount), 10);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("discountValue"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "100", "100.000"})
    void percentageBoundariesAccepted(String amount) {
        assertThat(validator.validate(new CreateVoucherRequest("FPT", "Discount", VoucherDiscountType.PERCENTAGE,
            new BigDecimal(amount), 0))).isEmpty();
    }

    @Test void fixedDiscountMayExceed100AndReachDecimalStorageBoundary() {
        assertThat(validator.validate(new CreateVoucherRequest("FPT", "Discount", VoucherDiscountType.FIXED,
            new BigDecimal("99999999999999999.99"), Integer.MAX_VALUE))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "voucherName", "discountType", "discountValue", "usageLimit"})
    void createRequiresEveryInputField(String field) throws Exception {
        var object = mapper.readTree(CREATE).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) object).remove(field);
        var request = mapper.treeToValue(object, CreateVoucherRequest.class);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test void rejectsBlankOversizedTextAndNegativeLimit() {
        for (var request : List.of(
            new CreateVoucherRequest("  ", "Name", VoucherDiscountType.FIXED, BigDecimal.ONE, 0),
            new CreateVoucherRequest("X".repeat(21), "Name", VoucherDiscountType.FIXED, BigDecimal.ONE, 0),
            new CreateVoucherRequest("FPT", "  ", VoucherDiscountType.FIXED, BigDecimal.ONE, 0),
            new CreateVoucherRequest("FPT", "X".repeat(256), VoucherDiscountType.FIXED, BigDecimal.ONE, 0),
            new CreateVoucherRequest("FPT", "Name", VoucherDiscountType.FIXED, BigDecimal.ONE, -1))) {
            assertThat(validator.validate(request)).isNotEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"voucherId", "usedCount", "reservedCount", "remainingCount", "version", "description", "expiresAt"})
    void rejectsUnknownAndServerOwnedFieldsForBothInputs(String field) throws Exception {
        String create = CREATE.stripTrailing().replace("}", ",\"" + field + "\":0}");
        assertThatThrownBy(() -> mapper.readValue(create, CreateVoucherRequest.class)).isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"usageLimit\":10,\"" + field + "\":0}", EditVoucherRequest.class))
            .isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "voucherName", "discountType", "discountValue", "usageLimit", "status"})
    void patchRejectsExplicitNullOnEveryEditableField(String field) {
        assertThatThrownBy(() -> mapper.readValue("{\"" + field + "\":null}", EditVoucherRequest.class))
            .isInstanceOf(JsonProcessingException.class);
    }

    @Test void patchKeepsOmittedFieldsAbsentAndAcceptsFalseAndZero() throws Exception {
        var patch = mapper.readValue("{\"usageLimit\":0,\"status\":false}", EditVoucherRequest.class);
        assertThat(validator.validate(patch)).isEmpty();
        assertThat(patch.usageLimit()).isZero();
        assertThat(patch.status()).isFalse();
        assertThat(patch.code()).isNull();
        assertThat(patch.voucherName()).isNull();
        assertThat(patch.discountType()).isNull();
        assertThat(patch.discountValue()).isNull();
    }

    @Test void patchRejectsEmptyObjectAndCannotSetInternalValidationProperty() throws Exception {
        assertThat(validator.validate(mapper.readValue("{}", EditVoucherRequest.class))).isNotEmpty();
        assertThatThrownBy(() -> mapper.readValue("{\"usageLimit\":1,\"notEmpty\":true}", EditVoucherRequest.class))
            .isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"code\":\"  \"}", "{\"voucherName\":\"  \"}", "{\"discountValue\":0}",
        "{\"discountValue\":1.001}", "{\"usageLimit\":-1}",
        "{\"discountType\":\"PERCENTAGE\",\"discountValue\":101}"
    })
    void patchRejectsInvalidSuppliedValues(String json) throws Exception {
        assertThat(validator.validate(mapper.readValue(json, EditVoucherRequest.class))).isNotEmpty();
    }

    @Test void patchNormalizesTextAndMoneyAndLeavesMergedValidationToCommand() throws Exception {
        var patch = mapper.readValue("""
            {"code":" hieu10 ","voucherName":" Name ","discountValue":20000.000}
            """, EditVoucherRequest.class);
        assertThat(patch.code()).isEqualTo("HIEU10");
        assertThat(patch.voucherName()).isEqualTo("Name");
        assertThat(validator.validate(patch)).isEmpty();
        // A fixed-value edit may be valid; the command must validate the current row's type.
        var mergedPercentage = new CreateVoucherRequest(patch.code(), patch.voucherName(),
            VoucherDiscountType.PERCENTAGE, patch.discountValue(), 10);
        assertThat(validator.validate(mergedPercentage)).anyMatch(v -> v.getPropertyPath().toString().equals("discountValue"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"usageLimit\":1.5}", "{\"usageLimit\":\"10\"}", "{\"usageLimit\":2147483648}",
        "{\"status\":\"false\"}", "{\"status\":0}", "{\"discountValue\":\"10\"}",
        "{\"discountType\":0}", "{\"discountType\":\"OTHER\"}"
    })
    void patchRejectsWrongJsonTypesAndIntegerOverflow(String json) {
        assertThatThrownBy(() -> mapper.readValue(json, EditVoucherRequest.class)).isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"code\":123}", "{\"voucherName\":false}", "{\"code\":[]}", "{\"voucherName\":{}}"})
    void createAndPatchRejectNonStringText(String json) throws Exception {
        assertThatThrownBy(() -> mapper.readValue(json, EditVoucherRequest.class)).isInstanceOf(JsonProcessingException.class);
        var create = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(CREATE);
        create.setAll((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json));
        assertThatThrownBy(() -> mapper.treeToValue(create, CreateVoucherRequest.class)).isInstanceOf(JsonProcessingException.class);
    }

    @Test void responseAndPageExposeContractFieldsWithoutEntityLeak() throws Exception {
        var voucher = new Voucher("VC1", "FPT", "FPT discount", "PERCENTAGE", new BigDecimal("10.00"),
            100, 70, 20, false);
        var response = VoucherResponse.from(voucher);
        var page = VoucherPage.from(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));
        var json = mapper.readTree(mapper.writeValueAsString(page));
        assertThat(json.size()).isEqualTo(5);
        assertThat(json.path("totalPages").intValue()).isEqualTo(1);
        assertThat(json.path("items").get(0).size()).isEqualTo(11);
        assertThat(json.path("items").get(0).path("remainingCount").intValue()).isEqualTo(10);
        assertThat(json.path("items").get(0).path("status").booleanValue()).isFalse();
        assertThat(json.path("items").get(0).path("version").longValue()).isZero();
    }
}
