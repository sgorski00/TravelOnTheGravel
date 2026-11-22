package pl.cwup.travelonthegravel.trip.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.routing.service.RoutingService;
import pl.cwup.travelonthegravel.trip.dto.request.UpdateSegmentRequest;
import pl.cwup.travelonthegravel.trip.dto.response.SegmentResponse;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService {

    private final TripService tripService;
    private final PointRepository pointRepository;
    private final SegmentRepository segmentRepository;
    private final RoutingService routingService;

    @Transactional(readOnly = true)
    public List<SegmentResponse> getSegments(Long userId, Long tripId) {
        tripService.findOwnTrip(userId, tripId);
        return segmentRepository.findAllByTripIdOrderByFromPointPosition(tripId).stream()
                .map(SegmentResponse::from)
                .toList();
    }

    @Transactional
    public SegmentResponse updateTransport(Long userId, Long tripId, Long segmentId, UpdateSegmentRequest request) {
        tripService.findOwnTrip(userId, tripId);
        final var segment = segmentRepository
                .findByIdAndTripId(segmentId, tripId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono odcinka"));
        if (segment.getTransportType() != request.transportType()) {
            segment.setTransportType(request.transportType());
            calculateRoute(segment);
        }
        return SegmentResponse.from(segment);
    }

    // Odcinek między tą samą parą punktów zostaje (z transportem) i liczy się od nowa tylko, gdy punkt się przesunął.
    @Transactional
    public void syncWithPoints(Trip trip, Set<Long> movedPointIds) {
        final var points = pointRepository.findAllByTripIdOrderByPosition(trip.getId());
        final var existing = new ArrayList<>(segmentRepository.findAllByTripIdOrderByFromPointPosition(trip.getId()));

        for (var i = 0; i < points.size() - 1; i++) {
            final var from = points.get(i);
            final var to = points.get(i + 1);
            final var kept = existing.stream()
                    .filter(segment -> segment.connects(from, to))
                    .findFirst();
            if (kept.isPresent()) {
                existing.remove(kept.get());
                if (movedPointIds.contains(from.getId()) || movedPointIds.contains(to.getId())) {
                    calculateRoute(kept.get());
                }
            } else {
                final var segment = new Segment(trip, from, to, TransportType.CAR);
                calculateRoute(segment);
                segmentRepository.save(segment);
            }
        }
        segmentRepository.deleteAll(existing);
    }

    private void calculateRoute(Segment segment) {
        switch (segment.getTransportType()) {
            case CAR -> setGroundRoute(segment);
            default -> applyStraightLine(segment, false);
        }
    }

    private void setGroundRoute(Segment segment) {
        final var from = segment.getFromPoint().coordinates();
        final var to = segment.getToPoint().coordinates();
        try {
            final var route = routingService.route(from, to, RouteDetail.FULL);
            if (route.isPresent()) {
                final var simplified = routingService
                        .route(from, to, RouteDetail.SIMPLIFIED)
                        .map(Route::geometry)
                        .orElse(route.get().geometry());
                segment.setRoute(route.get().distanceMeters(), route.get().geometry(), simplified, false);
                return;
            }
        } catch (ExternalServiceException ex) {
            log.warn("Nie udało się wyznaczyć trasy dla odcinka, zapisujemy linię prostą");
        }
        applyStraightLine(segment, true);
    }

    private static void applyStraightLine(Segment segment, boolean isFallback) {
        final Point from = segment.getFromPoint();
        final Point to = segment.getToPoint();
        final var line = GeoJsonLineString.straightLine(from.coordinates(), to.coordinates());
        segment.setRoute(from.coordinates().distanceMetersTo(to.coordinates()), line, line, isFallback);
    }
}
