package pl.cwup.travelonthegravel.place.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;
import pl.cwup.travelonthegravel.place.service.GeocodingService;

@Tag(name = "Miejsca", description = "Wyszukiwanie miejsc")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    public static final int MIN_QUERY_LENGTH = 3;

    private final GeocodingService geocodingService;

    @Operation(summary = "Podpowiedzi miejsc", description = "Maksymalnie 5 wyników.")
    @ApiResponse(responseCode = "200", description = "Lista miejsc")
    @ApiResponse(responseCode = "400", description = "Zapytanie krótsze niż 3 znaki")
    @ApiResponse(responseCode = "503", description = "Usługa map niedostępna")
    @GetMapping("/search")
    public List<PlaceResponse> search(
            @RequestParam
                    @NotBlank
                    @Size(min = MIN_QUERY_LENGTH, message = "Zapytanie musi mieć co najmniej {min} znaki")
                    String q) {
        return geocodingService.search(q);
    }
}
