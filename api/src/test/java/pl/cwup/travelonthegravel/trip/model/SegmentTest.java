package pl.cwup.travelonthegravel.trip.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import org.junit.jupiter.api.Test;

final class SegmentTest {

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");
    private final Point from = point(trip, "Warszawa", 0);
    private final Point to = point(trip, "Kraków", 1);

    @Test
    void constructorSetsFieldsWithoutRoute() {
        final var segment = new Segment(trip, from, to, TransportType.TRAIN);

        assertThat(segment.getTrip()).isSameAs(trip);
        assertThat(segment.getFromPoint()).isSameAs(from);
        assertThat(segment.getToPoint()).isSameAs(to);
        assertThat(segment.getTransportType()).isEqualTo(TransportType.TRAIN);
        assertThat(segment.getDistanceMeters()).isNull();
        assertThat(segment.getGeometry()).isNull();
    }

    @Test
    void setRouteChangesDistanceAndGeometry() {
        final var segment = new Segment(trip, from, to, TransportType.CAR);
        final var geometry = "{\"type\":\"LineString\",\"coordinates\":[[21.01,52.23],[19.94,50.06]]}";

        segment.setRoute(295_000L, geometry);

        assertThat(segment.getDistanceMeters()).isEqualTo(295_000L);
        assertThat(segment.getGeometry()).isEqualTo(geometry);
    }
}
