package pl.cwup.travelonthegravel.trip.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class PointRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PointRepository pointRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void visitedCountriesAreUniqueSortedWithoutEmptyAndOnlyFromOwnTrips() {
        final var jan = userRepository.save(user("jan@example.com", "jan"));
        final var ola = userRepository.save(user("ola@example.com", "ola"));
        final var wakacje = tripRepository.save(trip(jan, "Wakacje"));
        final var majowka = tripRepository.save(trip(jan, "Majówka"));
        final var cudza = tripRepository.save(trip(ola, "Cudza"));
        save(wakacje, "Warszawa", "PL", 0);
        save(wakacje, "Berlin", "DE", 1);
        save(wakacje, "Morze", null, 2);
        save(majowka, "Kraków", "PL", 0);
        save(cudza, "Paryż", "FR", 0);

        assertThat(pointRepository.findVisitedCountryCodes(jan.getId())).containsExactly("DE", "PL");
        assertThat(pointRepository.findVisitedCountryCodes(ola.getId())).containsExactly("FR");
    }

    private void save(Trip trip, String name, String countryCode, int position) {
        pointRepository.save(new Point(trip, name, 50.0, 20.0, countryCode, position));
    }
}
