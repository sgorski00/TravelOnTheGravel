package pl.cwup.travelonthegravel.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.profile.dto.response.PublicProfileResponse;
import pl.cwup.travelonthegravel.profile.dto.response.PublicTripResponse;
import pl.cwup.travelonthegravel.profile.dto.response.PublicUserResponse;
import pl.cwup.travelonthegravel.profile.service.PublicProfileService;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;

@Tag(name = "Inni użytkownicy", description = "Publiczne profile, tylko do odczytu")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @Operation(summary = "Wyszukiwanie profili", description = "Tylko publiczne, po fragmencie nazwy.")
    @ApiResponse(responseCode = "200", description = "Strona wyników")
    @ApiResponse(responseCode = "400", description = "Zapytanie krótsze niż 2 znaki")
    @GetMapping("/search")
    public Page<PublicUserResponse> search(
            @RequestParam @NotBlank @Size(min = 2, message = "Zapytanie musi mieć co najmniej {min} znaki") String q,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return publicProfileService.search(q, pageable);
    }

    @Operation(summary = "Profil użytkownika", description = "Nazwa i lista podróży.")
    @ApiResponse(responseCode = "200", description = "Profil")
    @ApiResponse(responseCode = "403", description = "Profil prywatny (PROFILE_PRIVATE)")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika")
    @GetMapping("/{id}")
    public PublicProfileResponse getProfile(@PathVariable Long id) {
        return publicProfileService.getProfile(id);
    }

    @Operation(summary = "Mapa podróży użytkownika")
    @ApiResponse(responseCode = "200", description = "GeoJSON wszystkich podróży")
    @ApiResponse(responseCode = "403", description = "Profil prywatny (PROFILE_PRIVATE)")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika")
    @GetMapping("/{id}/map")
    public MapResponse getMap(@PathVariable Long id, @RequestParam(defaultValue = "SIMPLIFIED") RouteDetail detail) {
        return publicProfileService.getMap(id, detail);
    }

    @Operation(summary = "Podróż użytkownika", description = "Szczegóły i mapa, tylko do odczytu.")
    @ApiResponse(responseCode = "200", description = "Podróż z mapą")
    @ApiResponse(responseCode = "403", description = "Profil prywatny (PROFILE_PRIVATE)")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego użytkownika albo podróży")
    @GetMapping("/{id}/trips/{tripId}")
    public PublicTripResponse getTrip(
            @PathVariable Long id,
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "SIMPLIFIED") RouteDetail detail) {
        return publicProfileService.getTrip(id, tripId, detail);
    }
}
