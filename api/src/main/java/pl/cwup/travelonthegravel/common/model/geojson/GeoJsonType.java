package pl.cwup.travelonthegravel.common.model.geojson;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeoJsonType {
    POINT("Point"),
    LINE_STRING("LineString"),
    FEATURE("Feature"),
    FEATURE_COLLECTION("FeatureCollection");

    @JsonValue
    private final String value;
}
