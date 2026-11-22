package pl.cwup.travelonthegravel.profile.dto.response;

import java.util.List;
import pl.cwup.travelonthegravel.trip.dto.response.TripSummaryResponse;

public record PublicProfileResponse(Long id, String username, List<TripSummaryResponse> trips) {}
