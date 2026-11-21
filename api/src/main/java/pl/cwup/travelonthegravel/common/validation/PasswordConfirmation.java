package pl.cwup.travelonthegravel.common.validation;

/** Żądanie z hasłem i jego powtórzeniem, sprawdzane przez {@link PasswordsMatch}. */
public interface PasswordConfirmation {

    String password();

    String passwordConfirmation();

    default String passwordConfirmationField() {
        return "passwordConfirmation";
    }
}
