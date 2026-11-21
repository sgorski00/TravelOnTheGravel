package pl.cwup.travelonthegravel.trip.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

final class TripTest {

    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 14);

    @Test
    void constructorSetsFields() {
        final var user = user("jan@example.com", "jan");

        final var trip = new Trip(user, "Wakacje", START, END, "Opis");

        assertThat(trip.getUser()).isSameAs(user);
        assertThat(trip.getName()).isEqualTo("Wakacje");
        assertThat(trip.getStartDate()).isEqualTo(START);
        assertThat(trip.getEndDate()).isEqualTo(END);
        assertThat(trip.getDescription()).isEqualTo("Opis");
    }

    @Test
    void setDatesChangesStartAndEndDate() {
        final var trip = new Trip(user("jan@example.com", "jan"), "Wakacje", START, null, null);

        trip.setDates(START.plusDays(1), END);

        assertThat(trip.getStartDate()).isEqualTo(START.plusDays(1));
        assertThat(trip.getEndDate()).isEqualTo(END);
    }

    @Test
    void isOwnedByChecksUserId() {
        final var user = user("jan@example.com", "jan");
        ReflectionTestUtils.setField(user, "id", 1L);
        final var trip = new Trip(user, "Wakacje", START, null, null);

        assertThat(trip.isOwnedBy(1L)).isTrue();
        assertThat(trip.isOwnedBy(2L)).isFalse();
    }
}
