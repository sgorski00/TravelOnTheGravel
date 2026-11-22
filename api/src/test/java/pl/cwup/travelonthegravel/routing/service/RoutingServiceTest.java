package pl.cwup.travelonthegravel.routing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.routing.client.OsrmApi;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmCode;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmGeometry;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmRoute;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmRouteResponse;
import pl.cwup.travelonthegravel.routing.model.Coordinates;
import pl.cwup.travelonthegravel.routing.model.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;

final class RoutingServiceTest {

    private static final Coordinates WARSAW = new Coordinates(52.2297, 21.0122);
    private static final Coordinates KRAKOW = new Coordinates(50.0647, 19.945);
    private static final List<List<Double>> COORDINATES = List.of(List.of(21.0122, 52.2297), List.of(19.945, 50.0647));
    private static final OsrmGeometry GEOMETRY = new OsrmGeometry("LineString", COORDINATES);

    private final OsrmApi osrmApi = mock(OsrmApi.class);
    private final RoutingService routingService = new RoutingService(osrmApi);

    @Test
    void routeReturnsRoundedDistanceAndGeometry() {
        when(osrmApi.route("21.012200,52.229700;19.945000,50.064700", "full"))
                .thenReturn(new OsrmRouteResponse(OsrmCode.OK, List.of(new OsrmRoute(294889.6, GEOMETRY))));

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.FULL))
                .contains(new Route(294890, GeoJsonLineString.of(COORDINATES)));
    }

    @Test
    void simplifiedDetailAsksOsrmForSimplifiedOverview() {
        when(osrmApi.route("21.012200,52.229700;19.945000,50.064700", "simplified"))
                .thenReturn(new OsrmRouteResponse(OsrmCode.OK, List.of(new OsrmRoute(294889.6, GEOMETRY))));

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.SIMPLIFIED)).isPresent();
    }

    @Test
    void routeWithoutRoutesIsEmpty() {
        when(osrmApi.route(anyString(), anyString())).thenReturn(new OsrmRouteResponse(OsrmCode.OK, List.of()));

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.FULL)).isEmpty();
    }

    @Test
    void noRouteResponseIsEmptyNotError() {
        when(osrmApi.route(anyString(), anyString())).thenThrow(badRequest(OsrmCode.NO_ROUTE));

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.FULL)).isEmpty();
    }

    @Test
    void noSegmentResponseIsEmptyNotError() {
        when(osrmApi.route(anyString(), anyString())).thenThrow(badRequest(OsrmCode.NO_SEGMENT));

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.FULL)).isEmpty();
    }

    @Test
    void otherBadRequestThrowsExternalServiceException() {
        when(osrmApi.route(anyString(), anyString())).thenThrow(badRequest(OsrmCode.INVALID_QUERY));

        assertThatThrownBy(() -> routingService.route(WARSAW, KRAKOW, RouteDetail.FULL))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void timeoutThrowsExternalServiceException() {
        when(osrmApi.route(anyString(), anyString())).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> routingService.route(WARSAW, KRAKOW, RouteDetail.FULL))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void rateLimitFallbackThrowsExternalServiceException() {
        final var ex = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("osrm"));

        assertThatThrownBy(() -> routingService.routeRateLimited(WARSAW, KRAKOW, RouteDetail.FULL, ex))
                .isInstanceOf(ExternalServiceException.class);
    }

    private static HttpClientErrorException badRequest(OsrmCode code) {
        final var ex = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
        ex.setBodyConvertFunction(type -> new OsrmRouteResponse(code, List.of()));
        return ex;
    }
}
