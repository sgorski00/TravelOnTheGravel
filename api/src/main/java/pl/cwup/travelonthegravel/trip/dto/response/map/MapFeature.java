package pl.cwup.travelonthegravel.trip.dto.response.map;

import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonGeometry;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonPoint;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonType;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;

public record MapFeature(GeoJsonType type, GeoJsonGeometry geometry, MapFeatureProperties properties) {

    public static MapFeature from(Point point) {
        return new MapFeature(GeoJsonType.FEATURE, GeoJsonPoint.of(point.coordinates()), PointProperties.from(point));
    }

    public static MapFeature from(Segment segment, RouteDetail detail) {
        final var geometry = detail == RouteDetail.SIMPLIFIED && segment.getGeometrySimplified() != null
                ? segment.getGeometrySimplified()
                : segment.getGeometry();
        return new MapFeature(GeoJsonType.FEATURE, geometry, SegmentProperties.from(segment));
    }
}
