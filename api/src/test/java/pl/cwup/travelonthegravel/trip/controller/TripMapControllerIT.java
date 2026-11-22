package pl.cwup.travelonthegravel.trip.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.place.service.GeocodingService;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.routing.service.RoutingService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class TripMapControllerIT {

    private static final String PASSWORD = "tajnehaslo";
    private static final GeoJsonLineString FULL =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(17.0, 51.0), List.of(20.0, 50.0)));
    private static final GeoJsonLineString SIMPLIFIED =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.0, 50.0)));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GeocodingService geocodingService;

    @MockitoBean
    private RoutingService routingService;

    private String janToken;
    private String olaToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));
        janToken = accessToken("jan@example.com");
        olaToken = accessToken("ola@example.com");
        when(geocodingService.countryCode(anyDouble(), anyDouble())).thenReturn(Optional.of("PL"));
        when(routingService.route(any(), any(), eq(RouteDetail.FULL)))
                .thenReturn(Optional.of(new Route(300_000, FULL)));
        when(routingService.route(any(), any(), eq(RouteDetail.SIMPLIFIED)))
                .thenReturn(Optional.of(new Route(300_000, SIMPLIFIED)));
    }

    @Test
    void tripMapIsGeoJsonFeatureCollectionWithNumberedPointsAndSegments() throws Exception {
        final var trip = createTrip(janToken, "Wakacje");
        addPoint(janToken, trip, "Warszawa", 52.0, 21.0);
        addPoint(janToken, trip, "Kraków", 50.0, 20.0);
        addPoint(janToken, trip, "Gdańsk", 54.0, 18.0);

        perform(janToken, get("/api/trips/" + trip + "/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features", hasSize(5)))
                .andExpect(jsonPath("$.features[*].type", everyItem(is("Feature"))))
                .andExpect(
                        jsonPath("$.features[?(@.properties.kind == 'POINT')].geometry.type", everyItem(is("Point"))))
                .andExpect(jsonPath("$.features[?(@.properties.kind == 'POINT')].properties.number")
                        .value(contains(1, 2, 3)))
                .andExpect(jsonPath("$.features[?(@.properties.kind == 'POINT')].properties.name")
                        .value(contains("Warszawa", "Kraków", "Gdańsk")))
                .andExpect(jsonPath("$.features[0].geometry.coordinates").value(contains(21.0, 52.0)))
                .andExpect(jsonPath(
                        "$.features[?(@.properties.kind == 'SEGMENT')].geometry.type", everyItem(is("LineString"))))
                .andExpect(jsonPath("$.features[3].properties.transportType").value("CAR"))
                .andExpect(jsonPath("$.features[3].properties.distanceMeters").value(300_000))
                .andExpect(jsonPath("$.features[3].properties.isFallback").value(false))
                .andExpect(jsonPath("$.bbox").value(contains(18.0, 50.0, 21.0, 54.0)));

        // Pełna trasa wychodzi poza punkty (długość 17.0), więc bbox też się poszerza.
        perform(janToken, get("/api/trips/" + trip + "/map").param("detail", "FULL"))
                .andExpect(jsonPath("$.bbox").value(contains(17.0, 50.0, 21.0, 54.0)));
    }

    @Test
    void detailChoosesSegmentGeometry() throws Exception {
        final var trip = createTrip(janToken, "Wakacje");
        addPoint(janToken, trip, "Warszawa", 52.0, 21.0);
        addPoint(janToken, trip, "Kraków", 50.0, 20.0);

        perform(janToken, get("/api/trips/" + trip + "/map"))
                .andExpect(jsonPath("$.features[2].geometry.coordinates", hasSize(2)));
        perform(janToken, get("/api/trips/" + trip + "/map").param("detail", "FULL"))
                .andExpect(jsonPath("$.features[2].geometry.coordinates", hasSize(3)));
        perform(janToken, get("/api/trips/" + trip + "/map").param("detail", "zle"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyTripMapHasNoFeaturesAndNoBbox() throws Exception {
        final var trip = createTrip(janToken, "Pusta");

        perform(janToken, get("/api/trips/" + trip + "/map"))
                .andExpect(jsonPath("$.features", hasSize(0)))
                .andExpect(jsonPath("$.bbox").doesNotExist());
    }

    @Test
    void otherUsersTripMapReturns404() throws Exception {
        final var trip = createTrip(olaToken, "Cudza");

        perform(janToken, get("/api/trips/" + trip + "/map")).andExpect(status().isNotFound());
    }

    @Test
    void userMapContainsOnlyOwnTripsWithTripIdAndName() throws Exception {
        final var wakacje = createTrip(janToken, "Wakacje");
        addPoint(janToken, wakacje, "Warszawa", 52.0, 21.0);
        final var majowka = createTrip(janToken, "Majówka");
        addPoint(janToken, majowka, "Kraków", 50.0, 20.0);
        final var cudza = createTrip(olaToken, "Cudza");
        addPoint(olaToken, cudza, "Berlin", 52.5, 13.4);

        perform(janToken, get("/api/users/me/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features", hasSize(2)))
                .andExpect(jsonPath("$.features[*].properties.tripName").value(contains("Wakacje", "Majówka")))
                .andExpect(jsonPath("$.features[*].properties.tripId")
                        .value(contains(wakacje.intValue(), majowka.intValue())));
    }

    private Long createTrip(String token, String name) throws Exception {
        final var result = perform(
                token,
                post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"startDate\": \"2026-07-01\"}".formatted(name)));
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void addPoint(String token, Long tripId, String name, double latitude, double longitude) throws Exception {
        perform(
                        token,
                        post("/api/trips/" + tripId + "/points")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"%s\", \"latitude\": %s, \"longitude\": %s}"
                                        .formatted(name, latitude, longitude)))
                .andExpect(status().isCreated());
    }

    private ResultActions perform(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String accessToken(String email) throws Exception {
        final var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
