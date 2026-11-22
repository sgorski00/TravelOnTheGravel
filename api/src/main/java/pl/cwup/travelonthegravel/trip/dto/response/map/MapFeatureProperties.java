package pl.cwup.travelonthegravel.trip.dto.response.map;

public sealed interface MapFeatureProperties permits PointProperties, SegmentProperties {

    MapFeatureKind kind();
}
