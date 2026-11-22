package pl.cwup.travelonthegravel.trip.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.routing.model.GeoJsonLineString;

final class SegmentTest {

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");
    private final Point from = withId(point(trip, "Warszawa", 0), 1L);
    private final Point to = withId(point(trip, "Kraków", 1), 2L);

    @Test
    void constructorSetsFieldsWithoutRoute() {
        final var segment = new Segment(trip, from, to, TransportType.TRAIN);

        assertThat(segment.getTrip()).isSameAs(trip);
        assertThat(segment.getFromPoint()).isSameAs(from);
        assertThat(segment.getToPoint()).isSameAs(to);
        assertThat(segment.getTransportType()).isEqualTo(TransportType.TRAIN);
        assertThat(segment.getDistanceMeters()).isNull();
        assertThat(segment.getGeometry()).isNull();
        assertThat(segment.isFallback()).isFalse();
    }

    @Test
    void setRouteChangesDistanceGeometriesAndFallback() {
        final var segment = new Segment(trip, from, to, TransportType.CAR);
        final var full =
                GeoJsonLineString.of(List.of(List.of(21.01, 52.23), List.of(20.5, 51.2), List.of(19.94, 50.06)));
        final var simplified = GeoJsonLineString.of(List.of(List.of(21.01, 52.23), List.of(19.94, 50.06)));

        segment.setRoute(295_000L, full, simplified, true);

        assertThat(segment.getDistanceMeters()).isEqualTo(295_000L);
        assertThat(segment.getGeometry()).isEqualTo(full);
        assertThat(segment.getGeometrySimplified()).isEqualTo(simplified);
        assertThat(segment.isFallback()).isTrue();
    }

    @Test
    void connectsChecksBothPointsAndDirection() {
        final var segment = new Segment(trip, from, to, TransportType.CAR);
        final var other = withId(point(trip, "Gdańsk", 2), 3L);

        assertThat(segment.connects(from, to)).isTrue();
        assertThat(segment.connects(to, from)).isFalse();
        assertThat(segment.connects(from, other)).isFalse();
    }

    private static Point withId(Point point, Long id) {
        ReflectionTestUtils.setField(point, "id", id);
        return point;
    }
}
