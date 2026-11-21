package pl.cwup.travelonthegravel.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.cwup.travelonthegravel.common.validation.PasswordConfirmation;
import pl.cwup.travelonthegravel.common.validation.PasswordsMatch;
import pl.cwup.travelonthegravel.user.model.User;

@PasswordsMatch
public record ChangePasswordRequest(
        @NotBlank String currentPassword,

        @NotBlank @Size(min = User.PASSWORD_MIN_LENGTH, message = "Hasło musi mieć co najmniej {min} znaków")
        String newPassword,

        @NotBlank String newPasswordConfirmation)
        implements PasswordConfirmation {

    @Override
    public String password() {
        return newPassword;
    }

    @Override
    public String passwordConfirmation() {
        return newPasswordConfirmation;
    }

    @Override
    public String passwordConfirmationField() {
        return "newPasswordConfirmation";
    }
}
