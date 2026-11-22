package pl.cwup.travelonthegravel.routing.dto.osrm;

import java.util.List;

public record OsrmGeometry(String type, List<List<Double>> coordinates) {}
