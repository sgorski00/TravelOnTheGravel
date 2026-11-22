package pl.cwup.travelonthegravel.routing.model;

import java.util.Locale;

public record Coordinates(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public String toLonLat() {
        return String.format(Locale.ROOT, "%.6f,%.6f", longitude, latitude);
    }

    // Odległość po powierzchni Ziemi w linii prostej (wzór haversine).
    public long distanceMetersTo(Coordinates other) {
        final var dLat = Math.toRadians(other.latitude - latitude);
        final var dLon = Math.toRadians(other.longitude - longitude);
        final var a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude))
                        * Math.cos(Math.toRadians(other.latitude))
                        * Math.pow(Math.sin(dLon / 2), 2);
        return Math.round(2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a)));
    }
}
