package pl.cwup.travelonthegravel.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.auth.dto.LoginRequest;
import pl.cwup.travelonthegravel.auth.dto.RefreshTokenRequest;
import pl.cwup.travelonthegravel.auth.dto.RegisterRequest;
import pl.cwup.travelonthegravel.auth.dto.TokenResponse;
import pl.cwup.travelonthegravel.auth.service.AuthService;
import pl.cwup.travelonthegravel.user.dto.UserResponse;

@Tag(name = "Auth", description = "Rejestracja, logowanie i tokeny")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Rejestracja nowego konta")
    @ApiResponse(responseCode = "201", description = "Konto utworzone")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane")
    @ApiResponse(responseCode = "409", description = "E-mail lub nazwa zajęta (EMAIL_TAKEN, USERNAME_TAKEN)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Logowanie", description = "Zwraca access token (JWT) i refresh token.")
    @ApiResponse(responseCode = "200", description = "Zalogowano")
    @ApiResponse(responseCode = "401", description = "Nieprawidłowy e-mail lub hasło")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Nowy access token", description = "Wydaje nowy access token na podstawie refresh tokena.")
    @ApiResponse(responseCode = "200", description = "Nowy access token")
    @ApiResponse(responseCode = "401", description = "Refresh token nieprawidłowy, wygasły lub unieważniony")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Wylogowanie", description = "Unieważnia refresh token.")
    @ApiResponse(responseCode = "204", description = "Wylogowano")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
