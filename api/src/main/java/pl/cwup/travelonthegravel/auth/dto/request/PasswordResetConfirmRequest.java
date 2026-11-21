package pl.cwup.travelonthegravel.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.cwup.travelonthegravel.common.validation.PasswordConfirmation;
import pl.cwup.travelonthegravel.common.validation.PasswordsMatch;
import pl.cwup.travelonthegravel.user.model.User;

@PasswordsMatch
public record PasswordResetConfirmRequest(
        @NotBlank String token,

        @NotBlank @Size(min = User.PASSWORD_MIN_LENGTH, message = "Hasło musi mieć co najmniej {min} znaków")
        String password,

        @NotBlank String passwordConfirmation)
        implements PasswordConfirmation {}
