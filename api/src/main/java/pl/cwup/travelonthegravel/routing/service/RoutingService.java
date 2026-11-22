package pl.cwup.travelonthegravel.routing.service;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.routing.client.OsrmApi;
import pl.cwup.travelonthegravel.routing.dto.osrm.OsrmRouteResponse;
import pl.cwup.travelonthegravel.routing.model.Coordinates;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final OsrmApi osrmApi;

    @RateLimiter(name = "osrm", fallbackMethod = "routeRateLimited")
    public Optional<Route> route(Coordinates from, Coordinates to, RouteDetail detail) {
        try {
            final var response = osrmApi.route(from.toLonLat() + ";" + to.toLonLat(), detail.getOsrmValue());
            return response == null ? Optional.empty() : response.firstRoute();
        } catch (HttpClientErrorException ex) {
            final var body = ex.getResponseBodyAs(OsrmRouteResponse.class);
            if (body != null && body.code() != null && body.code().isNoRoute()) {
                return Optional.empty();
            }
            throw unavailable(ex);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private static ExternalServiceException unavailable(RestClientException ex) {
        log.warn("Błąd zapytania do OSRM", ex);
        return new ExternalServiceException("Usługa wyznaczania tras jest chwilowo niedostępna");
    }

    Optional<Route> routeRateLimited(Coordinates from, Coordinates to, RouteDetail detail, RequestNotPermitted ex) {
        throw new ExternalServiceException("Usługa wyznaczania tras jest chwilowo przeciążona");
    }
}
