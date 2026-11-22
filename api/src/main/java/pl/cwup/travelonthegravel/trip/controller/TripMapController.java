package pl.cwup.travelonthegravel.trip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;
import pl.cwup.travelonthegravel.trip.service.TripMapService;

@Tag(name = "Mapa", description = "Dane do narysowania mapy w formacie GeoJSON")
@RestController
@RequiredArgsConstructor
public class TripMapController {

    private static final String DETAIL_DESCRIPTION =
            "Dokładność tras samochodowych: SIMPLIFIED (mało danych) albo FULL";

    private final TripMapService tripMapService;

    @Operation(summary = "Mapa podróży", description = "FeatureCollection z punktami, odcinkami i bbox.")
    @ApiResponse(responseCode = "200", description = "GeoJSON podróży")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @GetMapping("/api/trips/{tripId}/map")
    public MapResponse getTripMap(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tripId,
            @Parameter(description = DETAIL_DESCRIPTION) @RequestParam(defaultValue = "SIMPLIFIED")
                    RouteDetail detail) {
        return tripMapService.getTripMap(user.id(), tripId, detail);
    }

    @Operation(summary = "Mapa wszystkich moich podróży", description = "Każdy element ma tripId i tripName.")
    @ApiResponse(responseCode = "200", description = "GeoJSON wszystkich podróży")
    @GetMapping("/api/users/me/map")
    public MapResponse getUserMap(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = DETAIL_DESCRIPTION) @RequestParam(defaultValue = "SIMPLIFIED")
                    RouteDetail detail) {
        return tripMapService.getUserMap(user.id(), detail);
    }
}
