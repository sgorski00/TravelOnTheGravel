package pl.cwup.travelonthegravel.trip.dto.response;

import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;

public record SegmentResponse(
        Long id,
        Long fromPointId,
        Long toPointId,
        TransportType transportType,
        Long distanceMeters,
        boolean isFallback) {

    public static SegmentResponse from(Segment segment) {
        return new SegmentResponse(
                segment.getId(),
                segment.getFromPoint().getId(),
                segment.getToPoint().getId(),
                segment.getTransportType(),
                segment.getDistanceMeters(),
                segment.isFallback());
    }
}
