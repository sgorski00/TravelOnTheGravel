package pl.cwup.travelonthegravel.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.user.dto.request.ChangePasswordRequest;
import pl.cwup.travelonthegravel.user.dto.request.UpdateProfileRequest;
import pl.cwup.travelonthegravel.user.dto.response.UserResponse;
import pl.cwup.travelonthegravel.user.service.UserService;

@Tag(name = "Profil", description = "Profil zalogowanego użytkownika")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Mój profil")
    @ApiResponse(responseCode = "200", description = "Dane profilu")
    @ApiResponse(responseCode = "401", description = "Brak lub zły token")
    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getProfile(user.id());
    }

    @Operation(summary = "Edycja profilu", description = "Zmienia tylko przesłane pola.")
    @ApiResponse(responseCode = "200", description = "Profil zapisany")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane")
    @ApiResponse(responseCode = "409", description = "E-mail lub nazwa zajęta (EMAIL_TAKEN, USERNAME_TAKEN)")
    @PatchMapping
    public UserResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @Operation(summary = "Zmiana hasła", description = "Wylogowuje wszystkie sesje (refresh tokeny).")
    @ApiResponse(responseCode = "204", description = "Hasło zmienione")
    @ApiResponse(
            responseCode = "400",
            description = "Niepoprawne dane albo złe obecne hasło (INVALID_CURRENT_PASSWORD)")
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
    }
}
