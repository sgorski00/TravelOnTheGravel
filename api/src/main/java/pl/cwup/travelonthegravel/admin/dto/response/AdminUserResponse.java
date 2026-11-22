package pl.cwup.travelonthegravel.admin.dto.response;

import java.time.Instant;
import pl.cwup.travelonthegravel.common.model.Role;
import pl.cwup.travelonthegravel.user.model.User;

public record AdminUserResponse(
        Long id, String email, String username, Role role, boolean blocked, boolean isPublic, Instant createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.isBlocked(),
                user.isPublic(),
                user.getCreatedAt());
    }
}
