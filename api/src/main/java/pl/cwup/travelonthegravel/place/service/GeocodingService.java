package pl.cwup.travelonthegravel.place.service;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.place.client.NominatimApi;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    public static final int MAX_RESULTS = 5;

    private final NominatimApi nominatimApi;

    @Cacheable(cacheNames = "places-search", key = "#query.trim().toLowerCase()")
    @RateLimiter(name = "nominatim", fallbackMethod = "searchRateLimited")
    public List<PlaceResponse> search(String query) {
        final var places = call(() -> nominatimApi.search(query.trim(), MAX_RESULTS));
        return places == null
                ? List.of()
                : places.stream().map(NominatimPlace::toPlaceResponse).toList();
    }

    @Cacheable(cacheNames = "places-country", key = "#latitude + ',' + #longitude", unless = "#result == null")
    @RateLimiter(name = "nominatim", fallbackMethod = "countryCodeRateLimited")
    public Optional<String> countryCode(double latitude, double longitude) {
        return Optional.ofNullable(call(() -> nominatimApi.reverse(latitude, longitude)))
                .map(NominatimPlace::countryCode);
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException ex) {
            log.warn("Błąd zapytania do Nominatim", ex);
            throw new ExternalServiceException("Usługa map jest chwilowo niedostępna");
        }
    }

    List<PlaceResponse> searchRateLimited(String query, RequestNotPermitted ex) {
        throw rateLimited();
    }

    Optional<String> countryCodeRateLimited(double latitude, double longitude, RequestNotPermitted ex) {
        throw rateLimited();
    }

    private static ExternalServiceException rateLimited() {
        return new ExternalServiceException("Usługa map jest chwilowo przeciążona");
    }
}
