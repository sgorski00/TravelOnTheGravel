package pl.cwup.travelonthegravel.trip.dto.response;

import pl.cwup.travelonthegravel.trip.model.Point;

public record PointResponse(Long id, String name, double latitude, double longitude, String countryCode, int position) {

    public static PointResponse from(Point point) {
        return new PointResponse(
                point.getId(),
                point.getName(),
                point.getLatitude(),
                point.getLongitude(),
                point.getCountryCode(),
                point.getPosition());
    }
}
