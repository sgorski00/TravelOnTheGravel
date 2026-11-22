package pl.cwup.travelonthegravel.routing.dto.osrm;

import pl.cwup.travelonthegravel.routing.model.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;

public record OsrmRoute(double distance, OsrmGeometry geometry) {

    public Route toRoute() {
        return new Route(Math.round(distance), GeoJsonLineString.of(geometry.coordinates()));
    }
}
