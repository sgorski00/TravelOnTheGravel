package pl.cwup.travelonthegravel.routing.model;

import java.util.Locale;

public record Coordinates(double latitude, double longitude) {

    public String toLonLat() {
        return String.format(Locale.ROOT, "%.6f,%.6f", longitude, latitude);
    }
}
