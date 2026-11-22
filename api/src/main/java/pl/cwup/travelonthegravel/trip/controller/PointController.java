package pl.cwup.travelonthegravel.trip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.trip.dto.request.PointOrderRequest;
import pl.cwup.travelonthegravel.trip.dto.request.PointRequest;
import pl.cwup.travelonthegravel.trip.dto.response.PointResponse;
import pl.cwup.travelonthegravel.trip.service.PointService;

@Tag(name = "Punkty podróży", description = "Punkty w moich podróżach")
@RestController
@RequestMapping("/api/trips/{tripId}/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "Punkty podróży", description = "W kolejności (position od 0).")
    @ApiResponse(responseCode = "200", description = "Lista punktów")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @GetMapping
    public List<PointResponse> getPoints(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long tripId) {
        return pointService.getPoints(user.id(), tripId);
    }

    @Operation(summary = "Nowy punkt", description = "Dodawany na koniec, kraj ustalany z współrzędnych.")
    @ApiResponse(responseCode = "201", description = "Punkt dodany")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PointResponse addPoint(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tripId,
            @Valid @RequestBody PointRequest request) {
        return pointService.addPoint(user.id(), tripId, request);
    }

    @Operation(summary = "Zmiana kolejności", description = "Przyjmuje wszystkie punkty podróży w nowej kolejności.")
    @ApiResponse(responseCode = "200", description = "Punkty w nowej kolejności")
    @ApiResponse(responseCode = "400", description = "Lista niepełna, z powtórzeniami lub obcymi punktami")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @PutMapping("/reorder")
    public List<PointResponse> reorderPoints(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tripId,
            @Valid @RequestBody PointOrderRequest request) {
        return pointService.reorderPoints(user.id(), tripId, request);
    }

    @Operation(summary = "Edycja punktu", description = "Przy zmianie współrzędnych kraj ustalany jest ponownie.")
    @ApiResponse(responseCode = "200", description = "Punkt zapisany")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego punktu albo podróż nie jest moja")
    @PutMapping("/{id}")
    public PointResponse updatePoint(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tripId,
            @PathVariable Long id,
            @Valid @RequestBody PointRequest request) {
        return pointService.updatePoint(user.id(), tripId, id, request);
    }

    @Operation(summary = "Usunięcie punktu", description = "Pozostałe punkty dostają ciągłe pozycje.")
    @ApiResponse(responseCode = "204", description = "Punkt usunięty")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego punktu albo podróż nie jest moja")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePoint(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long tripId, @PathVariable Long id) {
        pointService.deletePoint(user.id(), tripId, id);
    }
}
