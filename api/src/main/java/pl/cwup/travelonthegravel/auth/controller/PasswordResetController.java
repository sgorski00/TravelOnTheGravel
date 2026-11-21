package pl.cwup.travelonthegravel.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.auth.dto.request.PasswordResetConfirmRequest;
import pl.cwup.travelonthegravel.auth.dto.request.PasswordResetRequest;
import pl.cwup.travelonthegravel.auth.service.PasswordResetService;

@Tag(name = "Auth", description = "Rejestracja, logowanie i tokeny")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Prośba o reset hasła",
            description = "Wysyła mail z linkiem. Odpowiedź jest taka sama, nawet gdy konto nie istnieje.")
    @ApiResponse(responseCode = "200", description = "Przyjęto")
    @ApiResponse(responseCode = "400", description = "Niepoprawny e-mail")
    @PostMapping("/request")
    public void request(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @Operation(summary = "Ustawienie nowego hasła", description = "Wylogowuje wszystkie wcześniejsze sesje.")
    @ApiResponse(responseCode = "200", description = "Hasło zmienione")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane albo token nieprawidłowy, użyty lub wygasły")
    @PostMapping("/confirm")
    public void confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
    }
}
