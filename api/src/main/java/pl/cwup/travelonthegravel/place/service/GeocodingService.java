package pl.cwup.travelonthegravel.place.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.place.client.NominatimApi;
import pl.cwup.travelonthegravel.place.config.NominatimProperties;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;

@Slf4j
@Service
public class GeocodingService {

    public static final int MAX_RESULTS = 5;

    private final NominatimApi nominatimApi;
    private final Duration minInterval;
    private Instant lastRequestAt = Instant.EPOCH;

    public GeocodingService(NominatimApi nominatimApi, NominatimProperties properties) {
        this.nominatimApi = nominatimApi;
        this.minInterval = properties.minInterval();
    }

    @Cacheable(cacheNames = "places-search", key = "#query.trim().toLowerCase()")
    public List<PlaceResponse> search(String query) {
        final var places = call(() -> nominatimApi.search(query.trim(), MAX_RESULTS));
        return places == null
                ? List.of()
                : places.stream().map(PlaceResponse::from).toList();
    }

    @Cacheable(cacheNames = "places-country", key = "#latitude + ',' + #longitude", unless = "#result.isEmpty()")
    public Optional<String> countryCode(double latitude, double longitude) {
        return Optional.ofNullable(call(() -> nominatimApi.reverse(latitude, longitude)))
                .map(NominatimPlace::countryCode);
    }

    private <T> T call(Supplier<T> request) {
        waitForRateLimit();
        try {
            return request.get();
        } catch (RestClientException ex) {
            log.warn("Błąd zapytania do Nominatim", ex);
            throw new ExternalServiceException("Usługa map jest chwilowo niedostępna");
        }
    }

    private synchronized void waitForRateLimit() {
        final var waitUntil = lastRequestAt.plus(minInterval);
        final var now = Instant.now();
        if (now.isBefore(waitUntil)) {
            try {
                Thread.sleep(Duration.between(now, waitUntil));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = Instant.now();
    }
}
