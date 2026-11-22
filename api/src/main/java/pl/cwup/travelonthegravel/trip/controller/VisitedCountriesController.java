package pl.cwup.travelonthegravel.trip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.trip.dto.response.VisitedCountriesResponse;
import pl.cwup.travelonthegravel.trip.service.VisitedCountriesService;

@Tag(name = "Odwiedzone państwa", description = "Kraje z punktów moich podróży")
@RestController
@RequiredArgsConstructor
public class VisitedCountriesController {

    private final VisitedCountriesService visitedCountriesService;

    @Operation(
            summary = "Moje odwiedzone państwa",
            description = "Kody ISO alpha-2 z punktów wszystkich podróży, bez krajów tranzytowych.")
    @ApiResponse(responseCode = "200", description = "Lista kodów krajów")
    @GetMapping("/api/users/me/countries")
    public VisitedCountriesResponse getVisitedCountries(@AuthenticationPrincipal AuthenticatedUser user) {
        return visitedCountriesService.getVisitedCountries(user.id());
    }
}
