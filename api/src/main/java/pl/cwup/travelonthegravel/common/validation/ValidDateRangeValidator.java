package pl.cwup.travelonthegravel.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public boolean isValid(DateRange range, ConstraintValidatorContext context) {
        // Brak którejś z dat sprawdzają inne adnotacje (np. @NotNull na startDate).
        if (range.startDate() == null
                || range.endDate() == null
                || !range.endDate().isBefore(range.startDate())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
