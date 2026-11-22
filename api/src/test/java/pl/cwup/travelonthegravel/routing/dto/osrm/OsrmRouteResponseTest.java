package pl.cwup.travelonthegravel.routing.dto.osrm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;

final class OsrmRouteResponseTest {

    private static final List<List<Double>> COORDINATES = List.of(List.of(21.01, 52.23), List.of(19.94, 50.06));
    private static final OsrmGeometry GEOMETRY = new OsrmGeometry("LineString", COORDINATES);

    @Test
    void firstRouteHasRoundedDistance() {
        final var response = new OsrmRouteResponse(
                OsrmCode.OK, List.of(new OsrmRoute(294889.6, GEOMETRY), new OsrmRoute(300000, GEOMETRY)));

        assertThat(response.firstRoute()).contains(new Route(294890, GeoJsonLineString.of(COORDINATES)));
    }

    @Test
    void noRoutesGiveEmptyResult() {
        assertThat(new OsrmRouteResponse(OsrmCode.OK, List.of()).firstRoute()).isEmpty();
        assertThat(new OsrmRouteResponse(OsrmCode.OK, null).firstRoute()).isEmpty();
    }
}
