package pl.cwup.travelonthegravel.trip.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.segment;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.routing.model.GeoJsonLineString;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class TripRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void deletingTripDeletesPointsAndSegments() {
        final var user = userRepository.save(user("jan@example.com", "jan"));
        final var trip = tripRepository.save(trip(user, "Wakacje"));
        final var warsaw = pointRepository.save(point(trip, "Warszawa", 0));
        final var krakow = pointRepository.save(point(trip, "Kraków", 1));
        segmentRepository.save(segment(trip, warsaw, krakow, TransportType.TRAIN));

        tripRepository.deleteById(trip.getId());

        assertThat(pointRepository.count()).isZero();
        assertThat(segmentRepository.count()).isZero();
    }

    @Test
    void segmentGeometryIsSavedAndReadAsGeoJson() {
        final var user = userRepository.save(user("ola@example.com", "ola"));
        final var trip = tripRepository.save(trip(user, "Mapa"));
        final var from = pointRepository.save(point(trip, "Warszawa", 0));
        final var to = pointRepository.save(point(trip, "Kraków", 1));
        final var segment = segment(trip, from, to, TransportType.CAR);
        final var full =
                GeoJsonLineString.of(List.of(List.of(21.01, 52.23), List.of(20.5, 51.2), List.of(19.94, 50.06)));
        final var simplified = GeoJsonLineString.of(List.of(List.of(21.01, 52.23), List.of(19.94, 50.06)));
        segment.setRoute(294889, full, simplified, false);
        final var id = segmentRepository.saveAndFlush(segment).getId();

        final var saved = segmentRepository.findById(id).orElseThrow();

        assertThat(saved.getGeometry()).isEqualTo(full);
        assertThat(saved.getGeometrySimplified()).isEqualTo(simplified);
        assertThat(saved.getDistanceMeters()).isEqualTo(294889);
        assertThat(saved.isFallback()).isFalse();
    }
}
