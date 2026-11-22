package pl.cwup.travelonthegravel.trip.dto.response.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.stream.Stream;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonPoint;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonType;

// GeoJSON FeatureCollection. bbox według RFC 7946: [minLon, minLat, maxLon, maxLat], brak przy pustej mapie.
public record MapResponse(
        GeoJsonType type,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Double> bbox,
        List<MapFeature> features) {

    public static MapResponse of(List<MapFeature> features) {
        return new MapResponse(GeoJsonType.FEATURE_COLLECTION, boundingBox(features), features);
    }

    private static List<Double> boundingBox(List<MapFeature> features) {
        final var positions = features.stream().flatMap(MapResponse::positions).toList();
        if (positions.isEmpty()) {
            return null;
        }
        final var longitudes = positions.stream().map(List::getFirst).toList();
        final var latitudes =
                positions.stream().map(position -> position.get(1)).toList();
        return List.of(min(longitudes), min(latitudes), max(longitudes), max(latitudes));
    }

    private static Stream<List<Double>> positions(MapFeature feature) {
        return switch (feature.geometry()) {
            case GeoJsonPoint point -> Stream.of(point.coordinates());
            case GeoJsonLineString line -> line.coordinates().stream();
        };
    }

    private static double min(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    }

    private static double max(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
    }
}
