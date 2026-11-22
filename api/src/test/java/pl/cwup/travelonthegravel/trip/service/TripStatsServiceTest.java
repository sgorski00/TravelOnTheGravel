package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.segment;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
final class TripStatsServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @InjectMocks
    private TripStatsService tripStatsService;

    private final Trip trip2026 = trip("Wakacje", LocalDate.of(2026, 7, 1));
    private final Trip trip2025 = trip("Sylwester", LocalDate.of(2025, 12, 30));

    private void mockData() {
        final var warsaw = point(trip2026, "Warszawa", 52.2297, 21.0122, "PL");
        final var warsawAgain = point(trip2026, "Warszawa centrum", 52.2299, 21.0124, "PL");
        final var berlin = point(trip2026, "Berlin", 52.52, 13.405, "DE");
        final var sea = point(trip2026, "Bałtyk", 55.2, 17.5, null);
        final var prague = point(trip2025, "Praga", 50.0755, 14.4378, "CZ");
        final var vienna = point(trip2025, "Wiedeń", 48.2082, 16.3738, "AT");
        when(tripRepository.findAllByUserIdOrderByStartDateDesc(USER_ID)).thenReturn(List.of(trip2026, trip2025));
        when(pointRepository.findAllByTripUserIdOrderByTripIdAscPositionAsc(USER_ID))
                .thenReturn(List.of(warsaw, warsawAgain, berlin, sea, prague, vienna));
        when(segmentRepository.findAllByTripUserIdOrderByTripIdAscFromPointPositionAsc(USER_ID))
                .thenReturn(List.of(
                        segmentWithDistance(trip2026, warsaw, berlin, TransportType.CAR, 570_000),
                        segmentWithDistance(trip2026, berlin, sea, TransportType.PLANE, 400_000),
                        segmentWithDistance(trip2025, prague, vienna, TransportType.TRAIN, 250_000)));
    }

    @Test
    void wholeHistoryCountsEverything() {
        mockData();

        final var stats = tripStatsService.getStats(USER_ID, null);

        assertThat(stats.tripCount()).isEqualTo(2);
        assertThat(stats.countries()).containsExactly("AT", "CZ", "DE", "PL");
        assertThat(stats.countryCount()).isEqualTo(4);
        assertThat(stats.totalDistanceMeters()).isEqualTo(1_220_000);
        assertThat(stats.placeCount()).isEqualTo(5);
    }

    @Test
    void yearCountsOnlyTripsStartedInThatYear() {
        mockData();

        final var stats = tripStatsService.getStats(USER_ID, 2025);

        assertThat(stats.tripCount()).isEqualTo(1);
        assertThat(stats.countries()).containsExactly("AT", "CZ");
        assertThat(stats.totalDistanceMeters()).isEqualTo(250_000);
        assertThat(stats.placeCount()).isEqualTo(2);
    }

    @Test
    void distanceByTransportHasAllTypesAndSumsToTotal() {
        mockData();

        final var stats = tripStatsService.getStats(USER_ID, 2026);

        assertThat(stats.distanceByTransport())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        TransportType.CAR, 570_000L,
                        TransportType.PLANE, 400_000L,
                        TransportType.TRAIN, 0L,
                        TransportType.OTHER, 0L));
        assertThat(stats.distanceByTransport().values().stream()
                        .mapToLong(Long::longValue)
                        .sum())
                .isEqualTo(stats.totalDistanceMeters());
    }

    @Test
    void nearbyPointsCountAsOnePlace() {
        mockData();

        assertThat(tripStatsService.getStats(USER_ID, 2026).placeCount()).isEqualTo(3);
    }

    @Test
    void yearWithoutTripsReturnsZeros() {
        mockData();

        final var stats = tripStatsService.getStats(USER_ID, 2010);

        assertThat(stats.tripCount()).isZero();
        assertThat(stats.countryCount()).isZero();
        assertThat(stats.countries()).isEmpty();
        assertThat(stats.totalDistanceMeters()).isZero();
        assertThat(stats.distanceByTransport().values()).containsOnly(0L);
        assertThat(stats.placeCount()).isZero();
    }

    @Test
    void yearsAreDistinctFromNewest() {
        when(tripRepository.findAllByUserIdOrderByStartDateDesc(USER_ID))
                .thenReturn(List.of(trip2026, trip("Majówka", LocalDate.of(2026, 5, 1)), trip2025));

        assertThat(tripStatsService.getYears(USER_ID).years()).containsExactly(2026, 2025);
    }

    private static Trip trip(String name, LocalDate start) {
        return new Trip(user("jan@example.com", "jan"), name, start, null, null);
    }

    private static Point point(Trip trip, String name, double latitude, double longitude, String countryCode) {
        return new Point(trip, name, latitude, longitude, countryCode, 0);
    }

    private static Segment segmentWithDistance(
            Trip trip, Point from, Point to, TransportType transport, long distance) {
        final var segment = segment(trip, from, to, transport);
        segment.setRoute(distance, GeoJsonLineString.straightLine(from.coordinates(), to.coordinates()), null, false);
        return segment;
    }
}
