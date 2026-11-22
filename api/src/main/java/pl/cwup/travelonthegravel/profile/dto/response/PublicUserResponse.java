package pl.cwup.travelonthegravel.profile.dto.response;

import pl.cwup.travelonthegravel.user.model.User;

public record PublicUserResponse(Long id, String username) {

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(user.getId(), user.getUsername());
    }
}
