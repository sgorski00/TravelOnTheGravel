package pl.cwup.travelonthegravel.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.segment;

import com.jayway.jsonpath.JsonPath;
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
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class TripControllerIT {

    private static final String TRIPS_URL = "/api/trips";
    private static final String PASSWORD = "tajnehaslo";

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

    private String janToken;
    private String olaToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));
        janToken = accessToken("jan@example.com");
        olaToken = accessToken("ola@example.com");
    }

    @Test
    void createAndGetTrip() throws Exception {
        final var id = createTrip(janToken, "Wakacje", "2026-07-01", "2026-07-14", "Opis")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wakacje"))
                .andExpect(jsonPath("$.startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.endDate").value("2026-07-14"))
                .andExpect(jsonPath("$.description").value("Opis"));

        perform(janToken, get(TRIPS_URL + "/" + idOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wakacje"));
    }

    @Test
    void listContainsOnlyOwnTripsFromNewestStartDate() throws Exception {
        createTrip(janToken, "Stara", "2025-05-01", null, null);
        createTrip(janToken, "Nowa", "2026-08-01", null, null);
        createTrip(olaToken, "Cudza", "2026-09-01", null, null);

        perform(janToken, get(TRIPS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Nowa", "Stara")));
    }

    @Test
    void updateTripChangesFields() throws Exception {
        final var id = idOf(createTrip(janToken, "Wakacje", "2026-07-01", null, null));

        perform(
                        janToken,
                        put(TRIPS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Majówka", "2026-05-01", "2026-05-03", "Nowy opis")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Majówka"))
                .andExpect(jsonPath("$.endDate").value("2026-05-03"));
    }

    @Test
    void otherUsersTripReturns404ForEveryOperation() throws Exception {
        final var id = idOf(createTrip(olaToken, "Cudza", "2026-07-01", null, null));
        final var url = TRIPS_URL + "/" + id;

        perform(janToken, get(url)).andExpect(status().isNotFound());
        perform(
                        janToken,
                        put(url).contentType(MediaType.APPLICATION_JSON)
                                .content(body("Moja", "2026-07-01", null, null)))
                .andExpect(status().isNotFound());
        perform(janToken, delete(url)).andExpect(status().isNotFound());

        assertThat(tripRepository.findById(id)).isPresent();
    }

    @Test
    void endDateBeforeStartDateReturns400() throws Exception {
        createTrip(janToken, "Wakacje", "2026-07-10", "2026-07-01", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));
    }

    @Test
    void missingNameOrStartDateReturns400() throws Exception {
        createTrip(janToken, "", null, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteTripRemovesPointsAndSegments() throws Exception {
        final var id = idOf(createTrip(janToken, "Wakacje", "2026-07-01", null, null));
        final var trip = tripRepository.findById(id).orElseThrow();
        final var from = pointRepository.save(point(trip, "Warszawa", 0));
        final var to = pointRepository.save(point(trip, "Kraków", 1));
        segmentRepository.save(segment(trip, from, to, TransportType.TRAIN));

        perform(janToken, delete(TRIPS_URL + "/" + id)).andExpect(status().isNoContent());

        assertThat(tripRepository.findById(id)).isEmpty();
        assertThat(pointRepository.count()).isZero();
        assertThat(segmentRepository.count()).isZero();
    }

    @Test
    void tripsRequireLogin() throws Exception {
        mockMvc.perform(get(TRIPS_URL)).andExpect(status().isUnauthorized());
    }

    private ResultActions createTrip(String token, String name, String start, String end, String description)
            throws Exception {
        return perform(
                token,
                post(TRIPS_URL).contentType(MediaType.APPLICATION_JSON).content(body(name, start, end, description)));
    }

    private static String body(String name, String start, String end, String description) {
        return """
                {"name": %s, "startDate": %s, "endDate": %s, "description": %s}
                """.formatted(json(name), json(start), json(end), json(description));
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static Long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
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
