package pl.cwup.travelonthegravel.trip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.security.AuthenticatedUser;
import pl.cwup.travelonthegravel.trip.dto.request.UpdateSegmentRequest;
import pl.cwup.travelonthegravel.trip.dto.response.SegmentResponse;
import pl.cwup.travelonthegravel.trip.service.SegmentService;

@Tag(name = "Odcinki podróży", description = "Odcinki między kolejnymi punktami podróży")
@RestController
@RequestMapping("/api/trips/{tripId}/segments")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentService segmentService;

    @Operation(summary = "Odcinki podróży", description = "W kolejności punktów. Tworzone automatycznie.")
    @ApiResponse(responseCode = "200", description = "Lista odcinków")
    @ApiResponse(responseCode = "404", description = "Nie ma takiej podróży albo nie jest moja")
    @GetMapping
    public List<SegmentResponse> getSegments(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long tripId) {
        return segmentService.getSegments(user.id(), tripId);
    }

    @Operation(summary = "Zmiana środka transportu", description = "Przelicza dystans i trasę odcinka.")
    @ApiResponse(responseCode = "200", description = "Odcinek zapisany")
    @ApiResponse(responseCode = "400", description = "Niepoprawny środek transportu")
    @ApiResponse(responseCode = "404", description = "Nie ma takiego odcinka albo podróż nie jest moja")
    @PatchMapping("/{id}")
    public SegmentResponse updateTransport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tripId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSegmentRequest request) {
        return segmentService.updateTransport(user.id(), tripId, id, request);
    }
}
