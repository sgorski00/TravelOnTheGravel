package pl.cwup.travelonthegravel.trip.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import pl.cwup.travelonthegravel.routing.service.RoutingService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class VisitedCountriesControllerIT {

    private static final String PASSWORD = "tajnehaslo";
    private static final String COUNTRIES_URL = "/api/users/me/countries";

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

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        token = accessToken();
        when(geocodingService.countryCode(eq(52.0), anyDouble())).thenReturn(Optional.of("PL"));
        when(geocodingService.countryCode(eq(52.5), anyDouble())).thenReturn(Optional.of("DE"));
        when(geocodingService.countryCode(eq(55.0), anyDouble())).thenReturn(Optional.empty());
        when(routingService.route(any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void listChangesRightAfterAddingAndDeletingPoints() throws Exception {
        final var trip = createTrip("Wakacje");
        perform(get(COUNTRIES_URL)).andExpect(jsonPath("$.countryCodes", empty()));

        addPoint(trip, "Warszawa", 52.0);
        final var berlin = addPoint(trip, "Berlin", 52.5);
        addPoint(trip, "Kraków", 52.0);
        addPoint(trip, "Bałtyk", 55.0);
        perform(get(COUNTRIES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCodes").value(contains("DE", "PL")));

        perform(delete("/api/trips/" + trip + "/points/" + berlin)).andExpect(status().isNoContent());
        perform(get(COUNTRIES_URL)).andExpect(jsonPath("$.countryCodes").value(contains("PL")));
    }

    @Test
    void deletingTripRemovesItsCountries() throws Exception {
        final var wakacje = createTrip("Wakacje");
        addPoint(wakacje, "Warszawa", 52.0);
        final var berlin = createTrip("Berlin");
        addPoint(berlin, "Brama", 52.5);
        perform(get(COUNTRIES_URL)).andExpect(jsonPath("$.countryCodes").value(contains("DE", "PL")));

        perform(delete("/api/trips/" + berlin)).andExpect(status().isNoContent());

        perform(get(COUNTRIES_URL)).andExpect(jsonPath("$.countryCodes").value(contains("PL")));
    }

    @Test
    void countriesRequireLogin() throws Exception {
        mockMvc.perform(get(COUNTRIES_URL)).andExpect(status().isUnauthorized());
    }

    private Long createTrip(String name) throws Exception {
        return idOf(perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\", \"startDate\": \"2026-07-01\"}".formatted(name))));
    }

    private Long addPoint(Long tripId, String name, double latitude) throws Exception {
        return idOf(perform(post("/api/trips/" + tripId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"latitude\": %s, \"longitude\": 20.0}".formatted(name, latitude)))
                .andExpect(status().isCreated()));
    }

    private static Long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
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
