package pl.cwup.travelonthegravel.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.place.client.NominatimApi;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimAddress;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;

final class GeocodingServiceTest {

    private static final NominatimPlace KRAKOW = new NominatimPlace(
            "50.0619474",
            "19.9368564",
            "Kraków",
            "Kraków, województwo małopolskie, Polska",
            new NominatimAddress("pl"));

    private final NominatimApi nominatimApi = mock(NominatimApi.class);
    private final GeocodingService geocodingService = new GeocodingService(nominatimApi);

    @Test
    void searchMapsPlacesWithUppercaseCountryCode() {
        when(nominatimApi.search("Kraków", 5)).thenReturn(List.of(KRAKOW));

        assertThat(geocodingService.search("  Kraków "))
                .containsExactly(new PlaceResponse(
                        "Kraków", "Kraków, województwo małopolskie, Polska", 50.0619474, 19.9368564, "PL"));
    }

    @Test
    void searchUsesFirstPartOfDisplayNameWhenNameIsEmpty() {
        when(nominatimApi.search("Rynek", 5))
                .thenReturn(List.of(new NominatimPlace("1", "2", "", "Rynek Główny, Kraków", null)));

        assertThat(geocodingService.search("Rynek")).singleElement().satisfies(place -> {
            assertThat(place.name()).isEqualTo("Rynek Główny");
            assertThat(place.countryCode()).isNull();
        });
    }

    @Test
    void searchWithoutResultsReturnsEmptyList() {
        when(nominatimApi.search("xyzxyz", 5)).thenReturn(null);

        assertThat(geocodingService.search("xyzxyz")).isEmpty();
    }

    @Test
    void countryCodeReturnsUppercaseCode() {
        when(nominatimApi.reverse(50.06, 19.94)).thenReturn(KRAKOW);

        assertThat(geocodingService.countryCode(50.06, 19.94)).contains("PL");
    }

    @Test
    void countryCodeIsEmptyWhenNominatimCannotGeocode() {
        when(nominatimApi.reverse(0, -30)).thenReturn(new NominatimPlace(null, null, null, null, null));

        assertThat(geocodingService.countryCode(0, -30)).isEmpty();
    }

    @Test
    void nominatimErrorThrowsExternalServiceException() {
        when(nominatimApi.search(anyString(), anyInt())).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> geocodingService.search("Kraków")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void rateLimitFallbacksThrowExternalServiceException() {
        final var ex = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("nominatim"));

        assertThatThrownBy(() -> geocodingService.searchRateLimited("Kraków", ex))
                .isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> geocodingService.countryCodeRateLimited(50.06, 19.94, ex))
                .isInstanceOf(ExternalServiceException.class);
    }
}
