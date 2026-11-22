package pl.cwup.travelonthegravel.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import pl.cwup.travelonthegravel.place.service.GeocodingService;
import pl.cwup.travelonthegravel.routing.model.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.routing.service.RoutingService;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
final class SegmentControllerIT {

    private static final String PASSWORD = "tajnehaslo";
    private static final GeoJsonLineString ROAD =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.5, 51.0), List.of(20.0, 50.0)));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GeocodingService geocodingService;

    @MockitoBean
    private RoutingService routingService;

    private String janToken;
    private String olaToken;
    private String tripUrl;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.save(new User("jan@example.com", "jan", passwordEncoder.encode(PASSWORD)));
        userRepository.save(new User("ola@example.com", "ola", passwordEncoder.encode(PASSWORD)));
        janToken = accessToken("jan@example.com");
        olaToken = accessToken("ola@example.com");
        when(geocodingService.countryCode(anyDouble(), anyDouble())).thenReturn(Optional.of("PL"));
        when(routingService.route(any(), any(), any())).thenReturn(Optional.of(new Route(300_000, ROAD)));

        final var trip = perform(
                janToken,
                post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Wakacje\", \"startDate\": \"2026-07-01\"}"));
        tripUrl = "/api/trips/" + idOf(trip);
    }

    @Test
    void tripWithNPointsHasNMinusOneSegmentsInOrder() throws Exception {
        final var warsaw = idOf(addPoint("Warszawa", 52.0, 21.0));
        final var krakow = idOf(addPoint("Kraków", 50.0, 20.0));
        final var gdansk = idOf(addPoint("Gdańsk", 54.0, 18.0));

        perform(janToken, get(tripUrl + "/segments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fromPointId").value(contains(warsaw.intValue(), krakow.intValue())))
                .andExpect(jsonPath("$[*].toPointId").value(contains(krakow.intValue(), gdansk.intValue())))
                .andExpect(jsonPath("$[*].transportType").value(contains("CAR", "CAR")))
                .andExpect(jsonPath("$[0].distanceMeters").value(300_000))
                .andExpect(jsonPath("$[0].isFallback").value(false));
    }

    @Test
    void reorderAndDeleteKeepNMinusOneSegments() throws Exception {
        final var warsaw = idOf(addPoint("Warszawa", 52.0, 21.0));
        final var krakow = idOf(addPoint("Kraków", 50.0, 20.0));
        final var gdansk = idOf(addPoint("Gdańsk", 54.0, 18.0));

        perform(
                        janToken,
                        put(tripUrl + "/points/reorder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pointIds\": [%d, %d, %d]}".formatted(gdansk, warsaw, krakow)))
                .andExpect(status().isOk());
        perform(janToken, get(tripUrl + "/segments"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fromPointId").value(contains(gdansk.intValue(), warsaw.intValue())));

        perform(janToken, delete(tripUrl + "/points/" + warsaw)).andExpect(status().isNoContent());
        perform(janToken, get(tripUrl + "/segments"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromPointId").value(gdansk.intValue()))
                .andExpect(jsonPath("$[0].toPointId").value(krakow.intValue()));
        assertThat(segmentRepository.count()).isEqualTo(1);
    }

    @Test
    void changingTransportToPlaneUsesStraightLineDistance() throws Exception {
        addPoint("Warszawa", 52.2297, 21.0122);
        addPoint("Kraków", 50.0647, 19.945);
        final var segmentId = firstSegmentId();

        perform(
                        janToken,
                        patch(tripUrl + "/segments/" + segmentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"transportType\": \"PLANE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportType").value("PLANE"))
                .andExpect(jsonPath("$.isFallback").value(false));

        final var segment = segmentRepository.findById(segmentId).orElseThrow();
        assertThat(segment.getDistanceMeters()).isBetween(251_000L, 253_000L);
        assertThat(segment.getGeometry().coordinates()).hasSize(2);
    }

    @Test
    void carSegmentWithoutRouteIsFallback() throws Exception {
        when(routingService.route(any(), any(), any())).thenReturn(Optional.empty());
        addPoint("Oahu", 21.3069, -157.8583);
        addPoint("Big Island", 19.7241, -155.0868);

        perform(janToken, get(tripUrl + "/segments"))
                .andExpect(jsonPath("$[0].transportType").value("CAR"))
                .andExpect(jsonPath("$[0].isFallback").value(true));
    }

    @Test
    void unchangedSegmentsAreNotRecalculated() throws Exception {
        addPoint("Warszawa", 52.0, 21.0);
        addPoint("Kraków", 50.0, 20.0);
        clearInvocations(routingService);

        addPoint("Gdańsk", 54.0, 18.0);

        // Tylko nowy odcinek Kraków-Gdańsk: jedno zapytanie FULL i jedno SIMPLIFIED.
        verify(routingService).route(any(), any(), eq(RouteDetail.FULL));
        verify(routingService).route(any(), any(), eq(RouteDetail.SIMPLIFIED));
    }

    @Test
    void renamingPointDoesNotRecalculateSegments() throws Exception {
        final var warsaw = idOf(addPoint("Warszawa", 52.0, 21.0));
        addPoint("Kraków", 50.0, 20.0);
        clearInvocations(routingService);

        perform(
                        janToken,
                        put(tripUrl + "/points/" + warsaw)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Stolica\", \"latitude\": 52.0, \"longitude\": 21.0}"))
                .andExpect(status().isOk());

        verify(routingService, never()).route(any(), any(), any());
    }

    @Test
    void segmentsOfOtherUsersTripReturn404() throws Exception {
        addPoint("Warszawa", 52.0, 21.0);
        addPoint("Kraków", 50.0, 20.0);
        final var segmentId = firstSegmentId();

        perform(olaToken, get(tripUrl + "/segments")).andExpect(status().isNotFound());
        perform(
                        olaToken,
                        patch(tripUrl + "/segments/" + segmentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"transportType\": \"PLANE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidTransportTypeReturns400() throws Exception {
        addPoint("Warszawa", 52.0, 21.0);
        addPoint("Kraków", 50.0, 20.0);

        perform(
                        janToken,
                        patch(tripUrl + "/segments/" + firstSegmentId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"transportType\": \"ROWER\"}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions addPoint(String name, double latitude, double longitude) throws Exception {
        return perform(
                        janToken,
                        post(tripUrl + "/points")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"%s\", \"latitude\": %s, \"longitude\": %s}"
                                        .formatted(name, latitude, longitude)))
                .andExpect(status().isCreated());
    }

    private Long firstSegmentId() throws Exception {
        final var body = perform(janToken, get(tripUrl + "/segments"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
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
