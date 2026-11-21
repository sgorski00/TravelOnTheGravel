package pl.cwup.travelonthegravel.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

final class ValidDateRangeValidatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 1);

    @ValidDateRange
    record TestRange(LocalDate startDate, LocalDate endDate) implements DateRange {}

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void endAfterStartIsValid() {
        assertThat(validator.validate(new TestRange(DAY, DAY.plusDays(3)))).isEmpty();
    }

    @Test
    void sameDayIsValid() {
        assertThat(validator.validate(new TestRange(DAY, DAY))).isEmpty();
    }

    @Test
    void missingDatesAreValid() {
        assertThat(validator.validate(new TestRange(DAY, null))).isEmpty();
        assertThat(validator.validate(new TestRange(null, DAY))).isEmpty();
    }

    @Test
    void endBeforeStartIsInvalidOnEndDateField() {
        assertThat(validator.validate(new TestRange(DAY, DAY.minusDays(1))))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("endDate"));
    }
}
