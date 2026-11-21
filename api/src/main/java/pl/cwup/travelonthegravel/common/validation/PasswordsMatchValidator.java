package pl.cwup.travelonthegravel.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfirmation> {

    @Override
    public boolean isValid(PasswordConfirmation request, ConstraintValidatorContext context) {
        if (Objects.equals(request.password(), request.passwordConfirmation())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(request.passwordConfirmationField())
                .addConstraintViolation();
        return false;
    }
}
