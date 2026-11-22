package pl.cwup.travelonthegravel.trip.dto.response.map;

import pl.cwup.travelonthegravel.trip.model.Point;

// number: kolejność punktu w podróży liczona od 1.
public record PointProperties(
        MapFeatureKind kind, Long tripId, String tripName, Long pointId, int number, String name, String countryCode)
        implements MapFeatureProperties {

    public static PointProperties from(Point point) {
        return new PointProperties(
                MapFeatureKind.POINT,
                point.getTrip().getId(),
                point.getTrip().getName(),
                point.getId(),
                point.getPosition() + 1,
                point.getName(),
                point.getCountryCode());
    }
}
