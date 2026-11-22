package pl.cwup.travelonthegravel.routing.dto.osrm;

import java.util.List;
import java.util.Optional;
import pl.cwup.travelonthegravel.routing.model.Route;

public record OsrmRouteResponse(OsrmCode code, List<OsrmRoute> routes) {

    public Optional<Route> firstRoute() {
        return routes == null || routes.isEmpty()
                ? Optional.empty()
                : Optional.of(routes.getFirst().toRoute());
    }
}
