package pl.cwup.travelonthegravel.trip.service;

import jakarta.annotation.Nullable;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.trip.dto.response.StatsResponse;
import pl.cwup.travelonthegravel.trip.dto.response.StatsYearsResponse;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;

@Service
@RequiredArgsConstructor
public class TripStatsService {

    // 3 miejsca po przecinku to ok. 100 m, więc dwa punkty w tym samym miejscu liczymy jako jedno miejsce.
    private static final int PLACE_PRECISION = 3;

    private final TripRepository tripRepository;
    private final PointRepository pointRepository;
    private final SegmentRepository segmentRepository;

    // year == null oznacza całą historię. Podróż liczy się do roku swojej daty początku.
    @Transactional(readOnly = true)
    public StatsResponse getStats(Long userId, @Nullable Integer year) {
        final Predicate<Trip> inYear = trip -> year == null || trip.startYear() == year;

        final var tripCount = (int) tripRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .filter(inYear)
                .count();

        final var points = pointRepository.findAllByTripUserIdOrderByTripIdAscPositionAsc(userId).stream()
                .filter(point -> inYear.test(point.getTrip()))
                .toList();
        final var countries = points.stream()
                .map(Point::getCountryCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        final var placeCount = (int) points.stream()
                .map(point -> point.coordinates().round(PLACE_PRECISION))
                .distinct()
                .count();

        final var distanceByTransport = new EnumMap<TransportType, Long>(TransportType.class);
        for (final var transport : TransportType.values()) {
            distanceByTransport.put(transport, 0L);
        }
        segmentRepository.findAllByTripUserIdOrderByTripIdAscFromPointPositionAsc(userId).stream()
                .filter(segment -> inYear.test(segment.getTrip()) && segment.getDistanceMeters() != null)
                .forEach(segment ->
                        distanceByTransport.merge(segment.getTransportType(), segment.getDistanceMeters(), Long::sum));
        final var totalDistance =
                distanceByTransport.values().stream().mapToLong(Long::longValue).sum();

        return new StatsResponse(
                tripCount, countries.size(), countries, totalDistance, distanceByTransport, placeCount);
    }

    @Transactional(readOnly = true)
    public StatsYearsResponse getYears(Long userId) {
        final var years = tripRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(Trip::startYear)
                .distinct()
                .toList();
        return new StatsYearsResponse(years);
    }
}
