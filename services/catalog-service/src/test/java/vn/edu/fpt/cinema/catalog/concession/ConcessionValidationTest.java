package vn.edu.fpt.cinema.catalog.concession;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.ConcessionType;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class ConcessionValidationTest {
    static ValidatorFactory factory;
    static Validator validator;
    @BeforeAll static void setup() { factory=Validation.buildDefaultValidatorFactory(); validator=factory.getValidator(); }
    @AfterAll static void close() { factory.close(); }
    static CreateConcessionRequest request(String name, String description, String price, String image) {
        return new CreateConcessionRequest(ConcessionType.PC,name,description,new BigDecimal(price),image);
    }
    @Test void validRequestTrimsText() {
        var request=request("  Popcorn  ","  Sweet corn.  ","50000.00","popcorn.png");
        assertThat(request.name()).isEqualTo("Popcorn");
        assertThat(validator.validate(request)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"P","Póp","Pop!","   ","abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"})
    void rejectsInvalidNames(String name) {
        assertThat(validator.validate(request(name,"Sweet corn.","1","popcorn.png")))
            .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }
    @ParameterizedTest @ValueSource(strings={"abc","Sweet corn!","Tiếng Việt","     "})
    void rejectsInvalidDescriptions(String description) {
        assertThat(validator.validate(request("Popcorn",description,"1","popcorn.png")))
            .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","0.01","25000.50","1.001","100000000000000000"})
    void rejectsInvalidMoney(String price) {
        assertThat(validator.validate(request("Popcorn","Sweet corn.",price,"popcorn.png")))
            .anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }
    @ParameterizedTest @ValueSource(strings={"25000","25000.00","1.0","99999999999999999","5E+4"})
    void acceptsWholeVndForCreateAndUpdate(String price) {
        var create=request("Popcorn","Sweet corn.",price,"popcorn.png");
        var update=new UpdateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",
            new BigDecimal(price),"popcorn.png",0L);
        assertThat(validator.validate(create)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();
        assertThat(create.price()).isEqualByComparingTo(price);
        assertThat(update.price()).isEqualByComparingTo(price);
    }
    @ParameterizedTest @ValueSource(strings={"25000.50","0.01","1.001","0","-1","100000000000000000"})
    void updateRejectsInvalidMoney(String price) {
        var update=new UpdateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",
            new BigDecimal(price),"popcorn.png",0L);
        assertThat(validator.validate(update)).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }
    @Test void missingPriceProducesValidationErrorWithoutNormalizationFailure() {
        var create=new CreateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",null,"popcorn.png");
        var update=new UpdateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",null,"popcorn.png",0L);
        assertThat(validator.validate(create)).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
        assertThat(validator.validate(update)).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }
    @ParameterizedTest @ValueSource(strings={"../popcorn.png","/assets/img/concessions/popcorn.png","missing.png","https://example.com/a.png",""})
    void rejectsImagesOutsidePackagedAllowlist(String image) {
        assertThat(validator.validate(request("Popcorn","Sweet corn.","1",image)))
            .anyMatch(v -> v.getPropertyPath().toString().equals("img"));
    }
    @Test void updateRequiresNonNegativeVersion() {
        var missing=new UpdateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",BigDecimal.ONE,"popcorn.png",null);
        var negative=new UpdateConcessionRequest(ConcessionType.PC,"Popcorn","Sweet corn.",BigDecimal.ONE,"popcorn.png",-1L);
        assertThat(validator.validate(missing)).anyMatch(v -> v.getPropertyPath().toString().equals("version"));
        assertThat(validator.validate(negative)).anyMatch(v -> v.getPropertyPath().toString().equals("version"));
    }
}
