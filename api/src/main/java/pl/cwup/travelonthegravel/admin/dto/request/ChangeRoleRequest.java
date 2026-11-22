package pl.cwup.travelonthegravel.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.cwup.travelonthegravel.common.model.Role;

public record ChangeRoleRequest(@NotNull Role role) {}
