package pl.cwup.travelonthegravel.common.security;

import pl.cwup.travelonthegravel.common.model.Role;

public record AuthenticatedUser(Long id, String username, Role role) {}
