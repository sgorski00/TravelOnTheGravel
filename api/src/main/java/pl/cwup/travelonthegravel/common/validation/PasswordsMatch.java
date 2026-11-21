package pl.cwup.travelonthegravel.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Sprawdza, czy pole {@code passwordConfirmation} jest takie samo jak {@code password}. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatchValidator.class)
public @interface PasswordsMatch {

    String message() default "Hasła nie są takie same";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
