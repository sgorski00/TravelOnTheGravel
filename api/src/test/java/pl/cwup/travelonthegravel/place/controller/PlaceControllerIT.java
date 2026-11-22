package pl.cwup.travelonthegravel.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.place.client.NominatimApi;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimAddress;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class PlaceControllerIT {

    private static final NominatimPlace KRAKOW = new NominatimPlace(
            "50.06", "19.94", "Kraków", "Kraków, województwo małopolskie, Polska", new NominatimAddress("pl"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @MockitoBean
    private NominatimApi nominatimApi;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void searchKrakowReturnsPlaceWithCoordinatesAndPlCode() throws Exception {
        when(nominatimApi.search("Kraków", 5)).thenReturn(List.of(KRAKOW));

        search("Kraków")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kraków"))
                .andExpect(jsonPath("$[0].latitude").value(50.06))
                .andExpect(jsonPath("$[0].longitude").value(19.94))
                .andExpect(jsonPath("$[0].countryCode").value("PL"));
    }

    @Test
    void tooShortQueryReturns400() throws Exception {
        search("Kr")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("q"));
    }

    @Test
    void unavailableNominatimReturns503() throws Exception {
        when(nominatimApi.search(anyString(), anyInt())).thenThrow(new ResourceAccessException("Connection refused"));

        search("Kraków")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_ERROR"));
    }

    @Test
    void sameQueryIsTakenFromCache() throws Exception {
        when(nominatimApi.search("Kraków", 5)).thenReturn(List.of(KRAKOW));

        search("Kraków").andExpect(status().isOk());
        search(" kraków ").andExpect(status().isOk());

        verify(nominatimApi, times(1)).search(anyString(), anyInt());
    }

    @Test
    void nominatimIsCalledAtMostOncePerSecond() throws Exception {
        when(nominatimApi.search(anyString(), anyInt())).thenReturn(List.of(KRAKOW));

        // Limit liczy się w sekundowych oknach, więc 3 różne zapytania muszą zająć co najmniej ~1 s.
        final var start = System.nanoTime();
        search("Kraków").andExpect(status().isOk());
        search("Gdańsk").andExpect(status().isOk());
        search("Poznań").andExpect(status().isOk());

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThan(Duration.ofMillis(900));
    }

    @Test
    void cachedQueryDoesNotWaitForRateLimit() throws Exception {
        when(nominatimApi.search(anyString(), anyInt())).thenReturn(List.of(KRAKOW));
        search("Wrocław").andExpect(status().isOk());

        final var start = System.nanoTime();
        search("wrocław").andExpect(status().isOk());

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
    }

    @Test
    void osrmHasSameRateLimitAsNominatim() {
        final var config = rateLimiterRegistry.rateLimiter("osrm").getRateLimiterConfig();

        assertThat(config.getLimitForPeriod()).isEqualTo(1);
        assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void searchRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/places/search").param("q", "Kraków")).andExpect(status().isUnauthorized());
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/places/search").param("q", query).with(jwt()));
    }
}
