package pl.cwup.travelonthegravel.common.model.geojson;

import java.util.List;
import pl.cwup.travelonthegravel.common.model.Coordinates;

// Współrzędne w kolejności GeoJSON: [longitude, latitude].
public record GeoJsonPoint(GeoJsonType type, List<Double> coordinates) implements GeoJsonGeometry {

    public static GeoJsonPoint of(Coordinates coordinates) {
        return new GeoJsonPoint(GeoJsonType.POINT, List.of(coordinates.longitude(), coordinates.latitude()));
    }
}
