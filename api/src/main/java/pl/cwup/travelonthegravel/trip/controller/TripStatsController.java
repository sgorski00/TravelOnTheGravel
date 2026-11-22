package pl.cwup.travelonthegravel.trip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.trip.dto.response.StatsResponse;
import pl.cwup.travelonthegravel.trip.dto.response.StatsYearsResponse;
import pl.cwup.travelonthegravel.trip.service.TripStatsService;

@Tag(name = "Statystyki", description = "Statystyki moich podróży")
@RestController
@RequestMapping("/api/users/me/stats")
@RequiredArgsConstructor
public class TripStatsController {

    private final TripStatsService tripStatsService;

    @Operation(
            summary = "Moje statystyki",
            description = "Bez year cała historia. Podróż liczy się do roku swojej daty początku.")
    @ApiResponse(responseCode = "200", description = "Statystyki (rok bez podróży zwraca zera)")
    @GetMapping
    public StatsResponse getStats(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Rok, np. 2026") @RequestParam(required = false) Integer year) {
        return tripStatsService.getStats(user.id(), year);
    }

    @Operation(summary = "Lata z podróżami", description = "Od najnowszego.")
    @ApiResponse(responseCode = "200", description = "Lista lat")
    @GetMapping("/years")
    public StatsYearsResponse getYears(@AuthenticationPrincipal AuthenticatedUser user) {
        return tripStatsService.getYears(user.id());
    }
}
