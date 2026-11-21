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
import pl.cwup.travelonthegravel.trip.dto.request.TripRequest;
import pl.cwup.travelonthegravel.trip.dto.response.TripResponse;
import pl.cwup.travelonthegravel.trip.dto.response.TripSummaryResponse;
import pl.cwup.travelonthegravel.trip.service.TripService;

@Tag(name = "Podróże", description = "Własne podróże zalogowanego użytkownika")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "Lista moich podróży", description = "Od najnowszej daty początku.")
    @ApiResponse(responseCode = "200", description = "Lista podróży")
    @GetMapping
    public List<TripSummaryResponse> getTrips(@AuthenticationPrincipal AuthenticatedUser user) {
        return tripService.getTrips(user.id());
    }

    @Operation(summary = "Szczegóły podróży")
    @ApiResponse(responseCode = "200", description = "Podróż")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @GetMapping("/{id}")
    public TripResponse getTrip(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return tripService.getTrip(user.id(), id);
    }

    @Operation(summary = "Nowa podróż")
    @ApiResponse(responseCode = "201", description = "Podróż utworzona")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane, np. data końca przed datą początku")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse createTrip(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody TripRequest request) {
        return tripService.createTrip(user.id(), request);
    }

    @Operation(summary = "Edycja podróży")
    @ApiResponse(responseCode = "200", description = "Podróż zapisana")
    @ApiResponse(responseCode = "400", description = "Niepoprawne dane, np. data końca przed datą początku")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @PutMapping("/{id}")
    public TripResponse updateTrip(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request) {
        return tripService.updateTrip(user.id(), id, request);
    }

    @Operation(summary = "Usunięcie podróży", description = "Usuwa też punkty i odcinki podróży.")
    @ApiResponse(responseCode = "204", description = "Podróż usunięta")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        tripService.deleteTrip(user.id(), id);
    }
}
