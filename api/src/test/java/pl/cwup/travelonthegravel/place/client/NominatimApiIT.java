package pl.cwup.travelonthegravel.place.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.place.service.GeocodingService;

// Sprawdza konfigurację spring.http.serviceclient.nominatim.* na małym serwerze HTTP zamiast prawdziwego Nominatim.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class NominatimApiIT {

    private static final HttpServer SERVER;
    private static volatile String responseBody = "[]";
    private static volatile Duration responseDelay = Duration.ZERO;
    private static volatile HttpExchange lastRequest;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        SERVER.createContext("/", NominatimApiIT::handle);
        // Kilka wątków, żeby "wolna" odpowiedź z testu timeoutu nie blokowała kolejnych testów.
        SERVER.setExecutor(Executors.newCachedThreadPool());
        SERVER.start();
    }

    @DynamicPropertySource
    static void nominatimProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.http.serviceclient.nominatim.base-url",
                () -> "http://localhost:" + SERVER.getAddress().getPort());
        registry.add("spring.http.serviceclient.nominatim.read-timeout", () -> "500ms");
        registry.add("app.nominatim.min-interval", () -> "0s");
    }

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private CacheManager cacheManager;

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @BeforeEach
    void setUp() {
        responseDelay = Duration.ZERO;
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void searchSendsQueryWithUserAgentAndMapsResponse() {
        responseBody = """
                [{"lat": "50.0619474", "lon": "19.9368564", "name": "Kraków",
                  "display_name": "Kraków, województwo małopolskie, Polska", "address": {"country_code": "pl"}}]
                """;

        final var places = geocodingService.search("Kraków");

        assertThat(places).singleElement().satisfies(place -> {
            assertThat(place.name()).isEqualTo("Kraków");
            assertThat(place.countryCode()).isEqualTo("PL");
        });
        assertThat(lastRequest.getRequestURI().getPath()).isEqualTo("/search");
        assertThat(lastRequest.getRequestURI().getQuery())
                .contains("q=Kraków", "limit=5", "format=jsonv2", "addressdetails=1");
        assertThat(lastRequest.getRequestHeaders().getFirst("User-Agent")).startsWith("TravelOnTheGravel");
    }

    @Test
    void reverseSendsCoordinates() {
        responseBody = """
                {"address": {"country_code": "de"}}
                """;

        assertThat(geocodingService.countryCode(52.52, 13.40)).contains("DE");
        assertThat(lastRequest.getRequestURI().getPath()).isEqualTo("/reverse");
        assertThat(lastRequest.getRequestURI().getQuery()).contains("lat=52.52", "lon=13.4", "zoom=3");
    }

    @Test
    void timeoutThrowsExternalServiceException() {
        responseBody = "[]";
        responseDelay = Duration.ofSeconds(2);

        assertThatThrownBy(() -> geocodingService.search("Gdańsk")).isInstanceOf(ExternalServiceException.class);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        lastRequest = exchange;
        try {
            Thread.sleep(responseDelay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        final var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        } catch (IOException ignored) {
            // Klient mógł już zerwać połączenie po timeoucie.
        }
    }
}
