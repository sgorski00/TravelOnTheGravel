package pl.cwup.travelonthegravel.routing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.model.Coordinates;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonType;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.routing.service.RoutingService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "spring.http.serviceclient.osrm.read-timeout=500ms",
            "resilience4j.ratelimiter.instances.osrm.limit-for-period=100",
            "app.http.user-agent=TestAgent/1.0"
        })
final class OsrmApiIT {

    private static final Coordinates WARSAW = new Coordinates(52.2297, 21.0122);
    private static final Coordinates KRAKOW = new Coordinates(50.0647, 19.945);

    private static final HttpServer SERVER;
    private static volatile int responseStatus;
    private static volatile String responseBody;
    private static volatile Duration responseDelay;
    private static volatile HttpExchange lastRequest;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        SERVER.createContext("/", OsrmApiIT::handle);
        SERVER.setExecutor(Executors.newCachedThreadPool());
        SERVER.start();
    }

    @DynamicPropertySource
    static void osrmProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.http.serviceclient.osrm.base-url",
                () -> "http://localhost:" + SERVER.getAddress().getPort());
    }

    @Autowired
    private RoutingService routingService;

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @BeforeEach
    void setUp() {
        responseStatus = 200;
        responseDelay = Duration.ZERO;
    }

    @Test
    void routeWarsawKrakowReturnsDistanceAndRoadGeometry() {
        responseBody = """
                {"code": "Ok", "routes": [{"distance": 294889.3, "geometry": {"type": "LineString",
                  "coordinates": [[21.012233, 52.229679], [20.5, 51.2], [19.945, 50.0647]]}}]}
                """;

        final var route = routingService.route(WARSAW, KRAKOW, RouteDetail.FULL).orElseThrow();

        assertThat(route.distanceMeters()).isEqualTo(294889);
        assertThat(route.geometry().type()).isEqualTo(GeoJsonType.LINE_STRING);
        assertThat(route.geometry().coordinates()).hasSize(3).first().isEqualTo(List.of(21.012233, 52.229679));
        assertThat(lastRequest.getRequestURI().getPath())
                .isEqualTo("/route/v1/driving/21.012200,52.229700;19.945000,50.064700");
        assertThat(lastRequest.getRequestURI().getQuery()).contains("overview=full", "geometries=geojson");
        assertThat(lastRequest.getRequestHeaders().getFirst("User-Agent")).isEqualTo("TestAgent/1.0");
        assertThat(lastRequest.getRequestHeaders().getFirst("Accept-Encoding")).isEqualTo("gzip");
    }

    @Test
    void simplifiedDetailIsSentAsOverviewParameter() {
        responseBody = """
                {"code": "Ok", "routes": [{"distance": 294889.3, "geometry": {"type": "LineString",
                  "coordinates": [[21.012233, 52.229679], [19.945, 50.0647]]}}]}
                """;

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.SIMPLIFIED)).isPresent();
        assertThat(lastRequest.getRequestURI().getQuery()).contains("overview=simplified");
    }

    @Test
    void noRouteIsReturnedAsEmptyResult() {
        responseStatus = 400;
        responseBody = """
                {"message": "Impossible route between points", "code": "NoRoute"}
                """;

        assertThat(routingService.route(WARSAW, KRAKOW, RouteDetail.FULL)).isEmpty();
    }

    @Test
    void serverErrorThrowsExternalServiceException() {
        responseStatus = 500;
        responseBody = "{}";

        assertThatThrownBy(() -> routingService.route(WARSAW, KRAKOW, RouteDetail.FULL))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void timeoutThrowsExternalServiceException() {
        responseBody = "{\"code\": \"Ok\", \"routes\": []}";
        responseDelay = Duration.ofSeconds(2);

        assertThatThrownBy(() -> routingService.route(WARSAW, KRAKOW, RouteDetail.FULL))
                .isInstanceOf(ExternalServiceException.class);
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
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        } catch (IOException ignored) {
            // Klient mógł już zerwać połączenie po timeoucie.
        }
    }
}
