package pl.cwup.travelonthegravel.profile.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class PublicProfileControllerIT {

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
    private PasswordEncoder passwordEncoder;

    private String olaToken;
    private Long janId;
    private Long januszId;
    private Long olaId;
    private Long janTripId;
    private Long olaTripId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        final var jan = saveUser("jan@example.com", "jan", true);
        saveUser("janina@example.com", "Janina", true);
        final var janusz = saveUser("janusz@example.com", "janusz", false);
        final var ola = saveUser("ola@example.com", "ola", false);
        janId = jan.getId();
        januszId = janusz.getId();
        olaId = ola.getId();
        olaToken = accessToken("ola@example.com");

        final var janTrip = tripRepository.save(new Trip(jan, "Wakacje", LocalDate.of(2026, 7, 1), null, "Opis"));
        pointRepository.save(new Point(janTrip, "Warszawa", 52.23, 21.01, "PL", 0));
        janTripId = janTrip.getId();
        olaTripId = tripRepository
                .save(new Trip(ola, "Prywatna", LocalDate.of(2026, 7, 1), null, null))
                .getId();
    }

    @Test
    void searchReturnsOnlyPublicProfilesIgnoringCase() throws Exception {
        perform(get("/api/users/search").param("q", "JAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username").value(contains("jan", "Janina")))
                .andExpect(jsonPath("$.content[0].id").value(janId.intValue()))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(content().string(not(containsString("janusz"))))
                .andExpect(content().string(not(containsString("@example.com"))));
    }

    @Test
    void searchIsPaginated() throws Exception {
        perform(get("/api/users/search").param("q", "jan").param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content[*].username").value(contains("Janina")))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void pageSizeIsLimitedTo50() throws Exception {
        perform(get("/api/users/search").param("q", "jan").param("size", "500"))
                .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void tooShortQueryReturns400() throws Exception {
        perform(get("/api/users/search").param("q", "j"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void publicProfileHasUsernameAndTripsWithoutEmail() throws Exception {
        perform(get("/api/users/" + janId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(janId.intValue()))
                .andExpect(jsonPath("$.username").value("jan"))
                .andExpect(jsonPath("$.trips[*].name").value(contains("Wakacje")))
                .andExpect(content().string(not(containsString("@example.com"))));
    }

    @Test
    void publicMapAndTripAreAvailableWithoutEmail() throws Exception {
        perform(get("/api/users/" + janId + "/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features", hasSize(1)))
                .andExpect(content().string(not(containsString("@example.com"))));
        perform(get("/api/users/" + janId + "/trips/" + janTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trip.name").value("Wakacje"))
                .andExpect(jsonPath("$.map.features[0].properties.name").value("Warszawa"))
                .andExpect(content().string(not(containsString("@example.com"))));
    }

    @Test
    void privateProfileTripsAndMapReturn403() throws Exception {
        perform(get("/api/users/" + januszId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_PRIVATE"));
        perform(get("/api/users/" + olaId + "/map")).andExpect(status().isForbidden());
        perform(get("/api/users/" + olaId + "/trips/" + olaTripId)).andExpect(status().isForbidden());
    }

    @Test
    void unknownUserOrTripReturns404() throws Exception {
        perform(get("/api/users/999999")).andExpect(status().isNotFound());
        perform(get("/api/users/" + janId + "/trips/" + olaTripId)).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        perform(get("/api/users/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersTripCannotBeModified() throws Exception {
        perform(put("/api/trips/" + janTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Moja\", \"startDate\": \"2026-07-01\"}"))
                .andExpect(status().isNotFound());
        perform(delete("/api/trips/" + janTripId)).andExpect(status().isNotFound());
        perform(post("/api/trips/" + janTripId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"X\", \"latitude\": 1.0, \"longitude\": 2.0}"))
                .andExpect(status().isNotFound());

        perform(get("/api/users/" + janId + "/trips/" + janTripId))
                .andExpect(jsonPath("$.trip.name").value("Wakacje"));
    }

    @Test
    void ownMeEndpointsStillWork() throws Exception {
        perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ola"));
    }

    private User saveUser(String email, String username, boolean isPublic) {
        final var user = new User(email, username, passwordEncoder.encode(PASSWORD));
        user.setPublic(isPublic);
        return userRepository.save(user);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + olaToken));
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
