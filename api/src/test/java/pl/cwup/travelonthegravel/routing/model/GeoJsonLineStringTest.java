package pl.cwup.travelonthegravel.routing.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

final class GeoJsonLineStringTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void straightLineUsesLongitudeLatitudeOrder() {
        final var line = GeoJsonLineString.straightLine(new Coordinates(52.23, 21.01), new Coordinates(50.06, 19.94));

        assertThat(line.type()).isEqualTo(GeoJsonType.LINE_STRING);
        assertThat(line.coordinates()).containsExactly(List.of(21.01, 52.23), List.of(19.94, 50.06));
    }

    @Test
    void jsonUsesGeoJsonTypeName() {
        final var json = jsonMapper.writeValueAsString(GeoJsonLineString.of(List.of(List.of(21.01, 52.23))));

        assertThat(json).contains("\"type\":\"LineString\"");
        assertThat(jsonMapper.readValue(json, GeoJsonLineString.class).type()).isEqualTo(GeoJsonType.LINE_STRING);
    }
}
