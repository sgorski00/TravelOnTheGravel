package pl.cwup.travelonthegravel.trip.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import org.junit.jupiter.api.Test;

final class PointTest {

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");

    @Test
    void constructorSetsFields() {
        final var point = new Point(trip, "Warszawa", 52.23, 21.01, "PL", 0);

        assertThat(point.getTrip()).isSameAs(trip);
        assertThat(point.getName()).isEqualTo("Warszawa");
        assertThat(point.getLatitude()).isEqualTo(52.23);
        assertThat(point.getLongitude()).isEqualTo(21.01);
        assertThat(point.getCountryCode()).isEqualTo("PL");
        assertThat(point.getPosition()).isZero();
    }

    @Test
    void setLocationChangesCoordinatesAndCountry() {
        final var point = new Point(trip, "Punkt", 52.23, 21.01, "PL", 0);

        point.setLocation(52.52, 13.40, "DE");

        assertThat(point.getLatitude()).isEqualTo(52.52);
        assertThat(point.getLongitude()).isEqualTo(13.40);
        assertThat(point.getCountryCode()).isEqualTo("DE");
    }
}
