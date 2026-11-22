package pl.cwup.travelonthegravel.trip.dto.response;

import java.util.List;
import java.util.Map;
import pl.cwup.travelonthegravel.trip.model.TransportType;

public record StatsResponse(
        int tripCount,
        int countryCount,
        List<String> countries,
        long totalDistanceMeters,
        Map<TransportType, Long> distanceByTransport,
        int placeCount) {}
