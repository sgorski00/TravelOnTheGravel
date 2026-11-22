package pl.cwup.travelonthegravel.trip.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.cwup.travelonthegravel.TestData.segment;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class TripStatsControllerIT {

    private static final String PASSWORD = "tajnehaslo";
    private static final String STATS_URL = "/api/users/me/stats";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        final var jan = userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        final var ola = userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));
        token = accessToken();

        final var wakacje = tripRepository.save(new Trip(jan, "Wakacje", LocalDate.of(2026, 7, 1), null, null));
        final var warsaw = point(wakacje, "Warszawa", 52.2297, 21.0122, "PL", 0);
        final var berlin = point(wakacje, "Berlin", 52.52, 13.405, "DE", 1);
        segmentWithDistance(wakacje, warsaw, berlin, TransportType.CAR, 570_000);

        final var sylwester = tripRepository.save(
                new Trip(jan, "Sylwester", LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2), null));
        final var prague = point(sylwester, "Praga", 50.0755, 14.4378, "CZ", 0);
        final var warsawAgain = point(sylwester, "Warszawa", 52.2298, 21.0123, "PL", 1);
        segmentWithDistance(sylwester, prague, warsawAgain, TransportType.TRAIN, 520_000);

        final var cudza = tripRepository.save(new Trip(ola, "Cudza", LocalDate.of(2026, 3, 1), null, null));
        point(cudza, "Paryż", 48.8566, 2.3522, "FR", 0);
    }

    @Test
    void wholeHistoryStatsOfOwnTrips() throws Exception {
        perform(get(STATS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripCount").value(2))
                .andExpect(jsonPath("$.countries").value(contains("CZ", "DE", "PL")))
                .andExpect(jsonPath("$.countryCount").value(3))
                .andExpect(jsonPath("$.totalDistanceMeters").value(1_090_000))
                .andExpect(jsonPath("$.distanceByTransport.CAR").value(570_000))
                .andExpect(jsonPath("$.distanceByTransport.TRAIN").value(520_000))
                .andExpect(jsonPath("$.distanceByTransport.PLANE").value(0))
                .andExpect(jsonPath("$.distanceByTransport.OTHER").value(0))
                .andExpect(jsonPath("$.placeCount").value(3));
    }

    @Test
    void tripAcrossNewYearCountsToStartYear() throws Exception {
        perform(get(STATS_URL).param("year", "2025"))
                .andExpect(jsonPath("$.tripCount").value(1))
                .andExpect(jsonPath("$.countries").value(contains("CZ", "PL")))
                .andExpect(jsonPath("$.totalDistanceMeters").value(520_000));
        perform(get(STATS_URL).param("year", "2026"))
                .andExpect(jsonPath("$.tripCount").value(1))
                .andExpect(jsonPath("$.countries").value(contains("DE", "PL")))
                .andExpect(jsonPath("$.totalDistanceMeters").value(570_000));
    }

    @Test
    void yearWithoutTripsReturnsZeros() throws Exception {
        perform(get(STATS_URL).param("year", "2010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripCount").value(0))
                .andExpect(jsonPath("$.countryCount").value(0))
                .andExpect(jsonPath("$.countries", empty()))
                .andExpect(jsonPath("$.totalDistanceMeters").value(0))
                .andExpect(jsonPath("$.distanceByTransport.CAR").value(0))
                .andExpect(jsonPath("$.placeCount").value(0));
    }

    @Test
    void yearsListContainsYearsWithTrips() throws Exception {
        perform(get(STATS_URL + "/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years").value(contains(2026, 2025)));
    }

    @Test
    void invalidYearReturns400() throws Exception {
        perform(get(STATS_URL).param("year", "abc")).andExpect(status().isBadRequest());
    }

    private Point point(Trip trip, String name, double latitude, double longitude, String countryCode, int position) {
        return pointRepository.save(new Point(trip, name, latitude, longitude, countryCode, position));
    }

    private void segmentWithDistance(Trip trip, Point from, Point to, TransportType transport, long distance) {
        final var segment = segment(trip, from, to, transport);
        segment.setRoute(distance, GeoJsonLineString.straightLine(from.coordinates(), to.coordinates()), null, false);
        segmentRepository.save(segment);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String accessToken() throws Exception {
        final var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"jan@example.com\", \"password\": \"%s\"}".formatted(PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
