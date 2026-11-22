package pl.cwup.travelonthegravel.routing.model;

import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;

public record Route(long distanceMeters, GeoJsonLineString geometry) {}
