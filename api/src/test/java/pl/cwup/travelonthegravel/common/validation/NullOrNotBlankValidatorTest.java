package pl.cwup.travelonthegravel.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class NullOrNotBlankValidatorTest {

    record TestRequest(@NullOrNotBlank String name) {}

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nullIsValid() {
        assertThat(validator.validate(new TestRequest(null))).isEmpty();
    }

    @Test
    void textIsValid() {
        assertThat(validator.validate(new TestRequest("jan"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void blankIsInvalid(String value) {
        assertThat(validator.validate(new TestRequest(value)))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage()).isEqualTo("nie może być puste"));
    }
}
