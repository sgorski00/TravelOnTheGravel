package pl.cwup.travelonthegravel.trip.dto.response.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.common.model.Coordinates;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonGeometry;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonPoint;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonType;
import tools.jackson.databind.json.JsonMapper;

final class MapResponseTest {

    @Test
    void bboxCoversPointsAndWholeRouteLines() {
        final var point = feature(GeoJsonPoint.of(new Coordinates(52.0, 21.0)));
        final var line =
                feature(GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(14.0, 54.5), List.of(19.0, 49.0))));

        final var response = MapResponse.of(List.of(point, line));

        assertThat(response.type()).isEqualTo(GeoJsonType.FEATURE_COLLECTION);
        assertThat(response.bbox()).containsExactly(14.0, 49.0, 21.0, 54.5);
    }

    @Test
    void emptyMapHasNoBbox() {
        final var response = MapResponse.of(List.of());

        assertThat(response.bbox()).isNull();
        assertThat(JsonMapper.builder().build().writeValueAsString(response))
                .isEqualTo("{\"type\":\"FeatureCollection\",\"features\":[]}");
    }

    private static MapFeature feature(GeoJsonGeometry geometry) {
        return new MapFeature(GeoJsonType.FEATURE, geometry, null);
    }
}
