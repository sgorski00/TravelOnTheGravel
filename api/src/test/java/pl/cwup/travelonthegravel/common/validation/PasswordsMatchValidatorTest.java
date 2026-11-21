package pl.cwup.travelonthegravel.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

final class PasswordsMatchValidatorTest {

    @PasswordsMatch
    record TestRequest(String password, String passwordConfirmation) implements PasswordConfirmation {}

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void samePasswordsAreValid() {
        assertThat(validator.validate(new TestRequest("tajnehaslo", "tajnehaslo")))
                .isEmpty();
    }

    @Test
    void differentPasswordsAreInvalidOnConfirmationField() {
        final var violations = validator.validate(new TestRequest("tajnehaslo", "inne"));

        assertThat(violations).hasSize(1);
        final var violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("passwordConfirmation");
        assertThat(violation.getMessage()).isEqualTo("Hasła nie są takie same");
    }
}
