package pl.cwup.travelonthegravel.trip.service;

import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapFeature;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;

@Service
@RequiredArgsConstructor
public class TripMapService {

    private final TripService tripService;
    private final PointRepository pointRepository;
    private final SegmentRepository segmentRepository;

    @Transactional(readOnly = true)
    public MapResponse getTripMap(Long userId, Long tripId, RouteDetail detail) {
        tripService.findOwnTrip(userId, tripId);
        return map(
                pointRepository.findAllByTripIdOrderByPosition(tripId),
                segmentRepository.findAllByTripIdOrderByFromPointPosition(tripId),
                detail);
    }

    @Transactional(readOnly = true)
    public MapResponse getUserMap(Long userId, RouteDetail detail) {
        return map(
                pointRepository.findAllByTripUserIdOrderByTripIdAscPositionAsc(userId),
                segmentRepository.findAllByTripUserIdOrderByTripIdAscFromPointPositionAsc(userId),
                detail);
    }

    private static MapResponse map(List<Point> points, List<Segment> segments, RouteDetail detail) {
        final var features = Stream.concat(
                        points.stream().map(MapFeature::from),
                        segments.stream().map(segment -> MapFeature.from(segment, detail)))
                .toList();
        return MapResponse.of(features);
    }
}
