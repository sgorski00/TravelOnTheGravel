package pl.cwup.travelonthegravel.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.admin.dto.request.ChangeRoleRequest;
import pl.cwup.travelonthegravel.admin.dto.response.AdminUserResponse;
import pl.cwup.travelonthegravel.admin.service.AdminUserService;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;

@Tag(name = "Admin: użytkownicy", description = "Zarządzanie kontami, tylko dla roli ADMIN")
@RestController
@RequestMapping("/api/admin/users/{id}")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Zablokowanie konta", description = "Wylogowuje wszystkie sesje użytkownika.")
    @ApiResponse(responseCode = "200", description = "Konto zablokowane")
    @ApiResponse(responseCode = "400", description = "Próba zablokowania własnego konta (CANNOT_MODIFY_SELF)")
    @ApiResponse(responseCode = "403", description = "Brak roli ADMIN")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika")
    @PostMapping("/block")
    public AdminUserResponse block(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return adminUserService.block(admin.id(), id);
    }

    @Operation(summary = "Odblokowanie konta")
    @ApiResponse(responseCode = "200", description = "Konto odblokowane")
    @ApiResponse(responseCode = "400", description = "Próba zmiany własnego konta (CANNOT_MODIFY_SELF)")
    @ApiResponse(responseCode = "403", description = "Brak roli ADMIN")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika")
    @PostMapping("/unblock")
    public AdminUserResponse unblock(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return adminUserService.unblock(admin.id(), id);
    }

    @Operation(summary = "Zmiana roli", description = "Np. nadanie roli ADMIN. Wylogowuje sesje użytkownika.")
    @ApiResponse(responseCode = "200", description = "Rola zmieniona")
    @ApiResponse(responseCode = "400", description = "Niepoprawna rola albo zmiana własnej roli (CANNOT_MODIFY_SELF)")
    @ApiResponse(responseCode = "403", description = "Brak roli ADMIN")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika")
    @PutMapping("/role")
    public AdminUserResponse changeRole(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return adminUserService.changeRole(admin.id(), id, request.role());
    }
}
