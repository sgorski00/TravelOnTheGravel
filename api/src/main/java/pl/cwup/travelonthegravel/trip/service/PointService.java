package pl.cwup.travelonthegravel.trip.service;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.place.service.GeocodingService;
import pl.cwup.travelonthegravel.trip.dto.request.PointOrderRequest;
import pl.cwup.travelonthegravel.trip.dto.request.PointRequest;
import pl.cwup.travelonthegravel.trip.dto.response.PointResponse;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final TripService tripService;
    private final PointRepository pointRepository;
    private final GeocodingService geocodingService;

    @Transactional(readOnly = true)
    public List<PointResponse> getPoints(Long userId, Long tripId) {
        tripService.findOwnTrip(userId, tripId);
        return pointRepository.findAllByTripIdOrderByPosition(tripId).stream()
                .map(PointResponse::from)
                .toList();
    }

    @Transactional
    public PointResponse addPoint(Long userId, Long tripId, PointRequest request) {
        final var trip = tripService.findOwnTrip(userId, tripId);
        final var position = pointRepository.countByTripId(tripId);
        final var point = new Point(
                trip,
                request.name(),
                request.latitude(),
                request.longitude(),
                countryCode(request.latitude(), request.longitude()),
                position);
        return PointResponse.from(pointRepository.save(point));
    }

    @Transactional
    public PointResponse updatePoint(Long userId, Long tripId, Long pointId, PointRequest request) {
        final var point = findOwnPoint(userId, tripId, pointId);
        point.setName(request.name());
        if (!point.isAt(request.latitude(), request.longitude())) {
            point.setLocation(
                    request.latitude(), request.longitude(), countryCode(request.latitude(), request.longitude()));
        }
        return PointResponse.from(point);
    }

    @Transactional
    public void deletePoint(Long userId, Long tripId, Long pointId) {
        pointRepository.delete(findOwnPoint(userId, tripId, pointId));
        pointRepository.flush();
        renumber(pointRepository.findAllByTripIdOrderByPosition(tripId));
    }

    @Transactional
    public List<PointResponse> reorderPoints(Long userId, Long tripId, PointOrderRequest request) {
        tripService.findOwnTrip(userId, tripId);
        final var points = pointRepository.findAllByTripIdOrderByPosition(tripId).stream()
                .collect(Collectors.toMap(Point::getId, Function.identity()));

        final var ids = request.pointIds();
        if (ids.size() != points.size() || !points.keySet().equals(new HashSet<>(ids))) {
            throw new BadRequestException(
                    ErrorCode.INVALID_POINT_ORDER, "Lista musi zawierać wszystkie punkty podróży bez powtórzeń");
        }

        final var ordered = ids.stream().map(points::get).toList();
        renumber(ordered);
        return ordered.stream().map(PointResponse::from).toList();
    }

    private Point findOwnPoint(Long userId, Long tripId, Long pointId) {
        tripService.findOwnTrip(userId, tripId);
        return pointRepository
                .findByIdAndTripId(pointId, tripId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono punktu"));
    }

    private static void renumber(List<Point> points) {
        for (var i = 0; i < points.size(); i++) {
            points.get(i).setPosition(i);
        }
    }

    // Gdy Nominatim nie działa, zapisujemy punkt bez kraju, zamiast blokować dodanie punktu.
    private String countryCode(double latitude, double longitude) {
        try {
            return geocodingService.countryCode(latitude, longitude).orElse(null);
        } catch (ExternalServiceException ex) {
            log.warn("Nie udało się ustalić kraju dla {}, {}", latitude, longitude);
            return null;
        }
    }
}
