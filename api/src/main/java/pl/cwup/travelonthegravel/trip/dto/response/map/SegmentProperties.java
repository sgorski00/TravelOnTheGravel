package pl.cwup.travelonthegravel.trip.dto.response.map;

import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;

public record SegmentProperties(
        MapFeatureKind kind,
        Long tripId,
        String tripName,
        Long segmentId,
        TransportType transportType,
        Long distanceMeters,
        boolean isFallback)
        implements MapFeatureProperties {

    public static SegmentProperties from(Segment segment) {
        return new SegmentProperties(
                MapFeatureKind.SEGMENT,
                segment.getTrip().getId(),
                segment.getTrip().getName(),
                segment.getId(),
                segment.getTransportType(),
                segment.getDistanceMeters(),
                segment.isFallback());
    }
}
