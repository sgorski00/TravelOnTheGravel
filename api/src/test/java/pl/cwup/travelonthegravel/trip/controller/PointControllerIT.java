package pl.cwup.travelonthegravel.trip.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
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
import pl.cwup.travelonthegravel.place.service.GeocodingService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class PointControllerIT {

    private static final String PASSWORD = "tajnehaslo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GeocodingService geocodingService;

    private String janToken;
    private String olaToken;
    private String pointsUrl;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));
        janToken = accessToken("jan@example.com");
        olaToken = accessToken("ola@example.com");
        when(geocodingService.countryCode(anyDouble(), anyDouble())).thenReturn(Optional.of("PL"));

        final var trip = perform(
                janToken,
                post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Wakacje\", \"startDate\": \"2026-07-01\"}"));
        pointsUrl = "/api/trips/" + idOf(trip) + "/points";
    }

    @Test
    void pointsAreAddedAtTheEndWithCountryCode() throws Exception {
        addPoint("Warszawa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));
        addPoint("Kraków")
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.countryCode").value("PL"));

        perform(janToken, get(pointsUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Warszawa", "Kraków")))
                .andExpect(jsonPath("$[*].position").value(contains(0, 1)));
    }

    @Test
    void deletingPointKeepsPositionsContinuous() throws Exception {
        addPoint("Warszawa");
        final var krakow = idOf(addPoint("Kraków"));
        addPoint("Gdańsk");

        perform(janToken, delete(pointsUrl + "/" + krakow)).andExpect(status().isNoContent());

        perform(janToken, get(pointsUrl))
                .andExpect(jsonPath("$[*].name").value(contains("Warszawa", "Gdańsk")))
                .andExpect(jsonPath("$[*].position").value(contains(0, 1)));
    }

    @Test
    void reorderChangesPositions() throws Exception {
        final var warsaw = idOf(addPoint("Warszawa"));
        final var krakow = idOf(addPoint("Kraków"));
        final var gdansk = idOf(addPoint("Gdańsk"));

        reorder(gdansk, warsaw, krakow).andExpect(status().isOk());

        perform(janToken, get(pointsUrl))
                .andExpect(jsonPath("$[*].name").value(contains("Gdańsk", "Warszawa", "Kraków")))
                .andExpect(jsonPath("$[*].position").value(contains(0, 1, 2)));
    }

    @Test
    void reorderWithIncompleteOrForeignListReturns400() throws Exception {
        final var warsaw = idOf(addPoint("Warszawa"));
        final var krakow = idOf(addPoint("Kraków"));

        reorder(warsaw)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POINT_ORDER"));
        reorder(warsaw, krakow, 999_999L).andExpect(status().isBadRequest());
        reorder(warsaw, warsaw).andExpect(status().isBadRequest());
    }

    @Test
    void updatingCoordinatesSetsNewCountry() throws Exception {
        final var id = idOf(addPoint("Warszawa"));
        when(geocodingService.countryCode(52.52, 13.40)).thenReturn(Optional.of("DE"));

        perform(
                        janToken,
                        put(pointsUrl + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Berlin", 52.52, 13.40)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"))
                .andExpect(jsonPath("$.countryCode").value("DE"));
    }

    @Test
    void pointsInOtherUsersTripReturn404() throws Exception {
        final var id = idOf(addPoint("Warszawa"));

        perform(olaToken, get(pointsUrl)).andExpect(status().isNotFound());
        perform(
                        olaToken,
                        post(pointsUrl).contentType(MediaType.APPLICATION_JSON).content(body("X", 1.0, 2.0)))
                .andExpect(status().isNotFound());
        perform(
                        olaToken,
                        put(pointsUrl + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("X", 1.0, 2.0)))
                .andExpect(status().isNotFound());
        perform(olaToken, delete(pointsUrl + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void invalidCoordinatesReturn400() throws Exception {
        perform(
                        janToken,
                        post(pointsUrl).contentType(MediaType.APPLICATION_JSON).content(body("X", 91.0, 181.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions addPoint(String name) throws Exception {
        return perform(
                janToken,
                post(pointsUrl).contentType(MediaType.APPLICATION_JSON).content(body(name, 50.06, 19.94)));
    }

    private ResultActions reorder(Long... ids) throws Exception {
        final var list =
                String.join(", ", Arrays.stream(ids).map(String::valueOf).toList());
        return perform(
                janToken,
                put(pointsUrl + "/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointIds\": [" + list + "]}"));
    }

    private static String body(String name, double latitude, double longitude) {
        return "{\"name\": \"%s\", \"latitude\": %s, \"longitude\": %s}".formatted(name, latitude, longitude);
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
