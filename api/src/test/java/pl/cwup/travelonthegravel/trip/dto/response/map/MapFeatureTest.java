package pl.cwup.travelonthegravel.trip.dto.response.map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.segment;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonPoint;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonType;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;

final class MapFeatureTest {

    private static final GeoJsonLineString FULL =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.5, 51.0), List.of(20.0, 50.0)));
    private static final GeoJsonLineString SIMPLIFIED =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.0, 50.0)));

    private final Trip trip = withId(trip(user("jan@example.com", "jan"), "Wakacje"), 7L);
    private final Point warsaw = withId(new Point(trip, "Warszawa", 52.0, 21.0, "PL", 0), 1L);
    private final Point krakow = withId(new Point(trip, "Kraków", 50.0, 20.0, "PL", 1), 2L);

    @Test
    void pointFeatureHasNumberFromOneAndTripData() {
        final var feature = MapFeature.from(krakow);

        assertThat(feature.type()).isEqualTo(GeoJsonType.FEATURE);
        assertThat(feature.geometry()).isEqualTo(GeoJsonPoint.of(krakow.coordinates()));
        assertThat(feature.properties())
                .isEqualTo(new PointProperties(MapFeatureKind.POINT, 7L, "Wakacje", 2L, 2, "Kraków", "PL"));
    }

    @Test
    void segmentFeatureUsesGeometryForChosenDetail() {
        final var segment = segment(trip, warsaw, krakow, TransportType.CAR);
        segment.setRoute(294_000L, FULL, SIMPLIFIED, false);

        assertThat(MapFeature.from(segment, RouteDetail.FULL).geometry()).isEqualTo(FULL);
        assertThat(MapFeature.from(segment, RouteDetail.SIMPLIFIED).geometry()).isEqualTo(SIMPLIFIED);
        assertThat(MapFeature.from(segment, RouteDetail.FULL).properties())
                .isEqualTo(new SegmentProperties(
                        MapFeatureKind.SEGMENT, 7L, "Wakacje", null, TransportType.CAR, 294_000L, false));
    }

    @Test
    void segmentWithoutSimplifiedGeometryFallsBackToFull() {
        final var segment = segment(trip, warsaw, krakow, TransportType.CAR);
        segment.setRoute(294_000L, FULL, null, false);

        assertThat(MapFeature.from(segment, RouteDetail.SIMPLIFIED).geometry()).isEqualTo(FULL);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
