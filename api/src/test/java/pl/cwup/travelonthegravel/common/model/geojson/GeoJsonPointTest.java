package pl.cwup.travelonthegravel.common.model.geojson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.common.model.Coordinates;
import tools.jackson.databind.json.JsonMapper;

final class GeoJsonPointTest {

    @Test
    void ofUsesLongitudeLatitudeOrder() {
        final var point = GeoJsonPoint.of(new Coordinates(52.23, 21.01));

        assertThat(point.type()).isEqualTo(GeoJsonType.POINT);
        assertThat(point.coordinates()).isEqualTo(List.of(21.01, 52.23));
    }

    @Test
    void jsonUsesGeoJsonTypeName() {
        final var json =
                JsonMapper.builder().build().writeValueAsString(GeoJsonPoint.of(new Coordinates(52.23, 21.01)));

        assertThat(json).isEqualTo("{\"type\":\"Point\",\"coordinates\":[21.01,52.23]}");
    }
}
