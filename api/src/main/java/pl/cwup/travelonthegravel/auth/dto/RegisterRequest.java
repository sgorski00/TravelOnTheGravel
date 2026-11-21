package pl.cwup.travelonthegravel.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.cwup.travelonthegravel.common.validation.PasswordConfirmation;
import pl.cwup.travelonthegravel.common.validation.PasswordsMatch;
import pl.cwup.travelonthegravel.user.model.User;

@PasswordsMatch
public record RegisterRequest(
        @NotBlank @Email @Size(max = User.EMAIL_MAX_LENGTH) String email,
        @NotBlank @Size(max = User.USERNAME_MAX_LENGTH) String username,

        @NotBlank @Size(min = User.PASSWORD_MIN_LENGTH, message = "Hasło musi mieć co najmniej {min} znaków")
        String password,

        @NotBlank String passwordConfirmation)
        implements PasswordConfirmation {}
