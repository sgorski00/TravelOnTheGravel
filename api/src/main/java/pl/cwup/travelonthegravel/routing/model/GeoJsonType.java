package pl.cwup.travelonthegravel.routing.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeoJsonType {
    LINE_STRING("LineString");

    @JsonValue
    private final String value;
}
