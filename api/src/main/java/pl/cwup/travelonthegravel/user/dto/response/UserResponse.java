package pl.cwup.travelonthegravel.user.dto.response;

import java.time.Instant;
import pl.cwup.travelonthegravel.user.model.User;

public record UserResponse(Long id, String email, String username, boolean isPublic, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getUsername(), user.isPublic(), user.getCreatedAt());
    }
}
