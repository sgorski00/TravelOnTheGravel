package pl.cwup.travelonthegravel.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import pl.cwup.travelonthegravel.common.validation.NullOrNotBlank;
import pl.cwup.travelonthegravel.user.model.User;

public record UpdateProfileRequest(
        @NullOrNotBlank @Size(max = User.USERNAME_MAX_LENGTH)
        String username,

        @NullOrNotBlank @Email @Size(max = User.EMAIL_MAX_LENGTH)
        String email,

        Boolean isPublic) {}
