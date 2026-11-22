package pl.cwup.travelonthegravel.common.model.geojson;

import java.util.List;
import pl.cwup.travelonthegravel.common.model.Coordinates;

// Współrzędne w kolejności GeoJSON: [longitude, latitude].
public record GeoJsonLineString(GeoJsonType type, List<List<Double>> coordinates) implements GeoJsonGeometry {

    public static GeoJsonLineString of(List<List<Double>> coordinates) {
        return new GeoJsonLineString(GeoJsonType.LINE_STRING, coordinates);
    }

    public static GeoJsonLineString straightLine(Coordinates from, Coordinates to) {
        return of(List.of(List.of(from.longitude(), from.latitude()), List.of(to.longitude(), to.latitude())));
    }
}
