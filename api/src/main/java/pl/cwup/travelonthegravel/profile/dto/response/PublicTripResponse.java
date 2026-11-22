package pl.cwup.travelonthegravel.profile.dto.response;

import pl.cwup.travelonthegravel.trip.dto.response.TripResponse;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;

public record PublicTripResponse(TripResponse trip, MapResponse map) {}
