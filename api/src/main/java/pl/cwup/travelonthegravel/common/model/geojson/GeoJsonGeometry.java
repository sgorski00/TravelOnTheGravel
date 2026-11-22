package pl.cwup.travelonthegravel.common.model.geojson;

public sealed interface GeoJsonGeometry permits GeoJsonPoint, GeoJsonLineString {

    GeoJsonType type();
}
