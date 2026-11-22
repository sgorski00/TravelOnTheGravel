package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.segment;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.Route;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.routing.service.RoutingService;
import pl.cwup.travelonthegravel.trip.dto.request.UpdateSegmentRequest;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Segment;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;

@ExtendWith(MockitoExtension.class)
final class SegmentServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 10L;
    private static final GeoJsonLineString FULL =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.5, 51.0), List.of(20.0, 50.0)));
    private static final GeoJsonLineString SIMPLIFIED =
            GeoJsonLineString.of(List.of(List.of(21.0, 52.0), List.of(20.0, 50.0)));

    @Mock
    private TripService tripService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private RoutingService routingService;

    @InjectMocks
    private SegmentService segmentService;

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");
    private Point warsaw;
    private Point krakow;
    private Point gdansk;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        warsaw = withId(new Point(trip, "Warszawa", 52.0, 21.0, "PL", 0), 1L);
        krakow = withId(new Point(trip, "Kraków", 50.0, 20.0, "PL", 1), 2L);
        gdansk = withId(new Point(trip, "Gdańsk", 54.0, 18.0, "PL", 2), 3L);
    }

    @Test
    void syncWithPointsCreatesCarSegmentBetweenEachPairOfPoints() {
        mockState(List.of(warsaw, krakow, gdansk), List.of());
        mockRoute(294_000);

        segmentService.syncWithPoints(trip, Set.of());

        final var saved = ArgumentCaptor.forClass(Segment.class);
        verify(segmentRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(segment -> {
            assertThat(segment.getTransportType()).isEqualTo(TransportType.CAR);
            assertThat(segment.getDistanceMeters()).isEqualTo(294_000);
            assertThat(segment.getGeometry()).isEqualTo(FULL);
            assertThat(segment.getGeometrySimplified()).isEqualTo(SIMPLIFIED);
            assertThat(segment.isFallback()).isFalse();
        });
        assertThat(saved.getAllValues().get(0).connects(warsaw, krakow)).isTrue();
        assertThat(saved.getAllValues().get(1).connects(krakow, gdansk)).isTrue();
    }

    @Test
    void syncWithPointsKeepsUnchangedSegmentWithItsTransportAndDoesNotRecalculate() {
        final var plane = segment(trip, warsaw, krakow, TransportType.PLANE);
        plane.setRoute(1, FULL, SIMPLIFIED, false);
        mockState(List.of(warsaw, krakow), List.of(plane));

        segmentService.syncWithPoints(trip, Set.of());

        assertThat(plane.getTransportType()).isEqualTo(TransportType.PLANE);
        assertThat(plane.getDistanceMeters()).isEqualTo(1);
        verify(segmentRepository, never()).save(any());
        verify(segmentRepository).deleteAll(List.of());
        verify(routingService, never()).route(any(), any(), any());
    }

    @Test
    void syncWithPointsRecalculatesSegmentWhosePointMoved() {
        final var car = segment(trip, warsaw, krakow, TransportType.CAR);
        car.setRoute(1, FULL, SIMPLIFIED, false);
        mockState(List.of(warsaw, krakow), List.of(car));
        mockRoute(250_000);

        segmentService.syncWithPoints(trip, Set.of(krakow.getId()));

        assertThat(car.getDistanceMeters()).isEqualTo(250_000);
    }

    @Test
    void syncWithPointsDeletesSegmentsThatNoLongerConnectNeighbours() {
        final var old = segment(trip, warsaw, gdansk, TransportType.TRAIN);
        mockState(List.of(warsaw, krakow), List.of(old));
        mockRoute(294_000);

        segmentService.syncWithPoints(trip, Set.of());

        verify(segmentRepository).deleteAll(List.of(old));
    }

    @Test
    void syncWithPointsWithOnePointHasNoSegments() {
        mockState(List.of(warsaw), List.of());

        segmentService.syncWithPoints(trip, Set.of());

        verify(segmentRepository, never()).save(any());
    }

    @Test
    void carWithoutRouteGetsStraightLineAndFallbackFlag() {
        mockState(List.of(warsaw, krakow), List.of());
        when(routingService.route(any(), any(), eq(RouteDetail.FULL))).thenReturn(Optional.empty());

        segmentService.syncWithPoints(trip, Set.of());

        final var segment = savedSegment();
        assertThat(segment.isFallback()).isTrue();
        assertThat(segment.getDistanceMeters()).isEqualTo(warsaw.coordinates().distanceMetersTo(krakow.coordinates()));
        assertThat(segment.getGeometry())
                .isEqualTo(GeoJsonLineString.straightLine(warsaw.coordinates(), krakow.coordinates()));
    }

    @Test
    void carWhenOsrmIsUnavailableGetsFallback() {
        mockState(List.of(warsaw, krakow), List.of());
        when(routingService.route(any(), any(), eq(RouteDetail.FULL)))
                .thenThrow(new ExternalServiceException("Usługa wyznaczania tras jest chwilowo niedostępna"));

        segmentService.syncWithPoints(trip, Set.of());

        assertThat(savedSegment().isFallback()).isTrue();
    }

    @Test
    void updateTransportToPlaneUsesStraightLineWithoutRouting() {
        final var segment = segment(trip, warsaw, krakow, TransportType.CAR);
        mockOwnSegment(segment);

        final var response =
                segmentService.updateTransport(USER_ID, TRIP_ID, 5L, new UpdateSegmentRequest(TransportType.PLANE));

        assertThat(response.transportType()).isEqualTo(TransportType.PLANE);
        assertThat(response.distanceMeters()).isEqualTo(warsaw.coordinates().distanceMetersTo(krakow.coordinates()));
        assertThat(response.isFallback()).isFalse();
        assertThat(segment.getGeometry())
                .isEqualTo(GeoJsonLineString.straightLine(warsaw.coordinates(), krakow.coordinates()));
        verify(routingService, never()).route(any(), any(), any());
    }

    @Test
    void updateTransportToSameValueDoesNotRecalculate() {
        final var segment = segment(trip, warsaw, krakow, TransportType.CAR);
        segment.setRoute(294_000, FULL, SIMPLIFIED, false);
        mockOwnSegment(segment);

        segmentService.updateTransport(USER_ID, TRIP_ID, 5L, new UpdateSegmentRequest(TransportType.CAR));

        assertThat(segment.getDistanceMeters()).isEqualTo(294_000);
        verify(routingService, never()).route(any(), any(), any());
    }

    @Test
    void updateTransportOfUnknownSegmentThrowsNotFound() {
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenReturn(trip);
        when(segmentRepository.findByIdAndTripId(5L, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> segmentService.updateTransport(
                        USER_ID, TRIP_ID, 5L, new UpdateSegmentRequest(TransportType.PLANE)))
                .isInstanceOf(NotFoundException.class);
    }

    private void mockState(List<Point> points, List<Segment> segments) {
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID)).thenReturn(points);
        when(segmentRepository.findAllByTripIdOrderByFromPointPosition(TRIP_ID)).thenReturn(new ArrayList<>(segments));
    }

    private void mockRoute(long distance) {
        when(routingService.route(any(), any(), eq(RouteDetail.FULL)))
                .thenReturn(Optional.of(new Route(distance, FULL)));
        when(routingService.route(any(), any(), eq(RouteDetail.SIMPLIFIED)))
                .thenReturn(Optional.of(new Route(distance, SIMPLIFIED)));
    }

    private void mockOwnSegment(Segment segment) {
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenReturn(trip);
        when(segmentRepository.findByIdAndTripId(5L, TRIP_ID)).thenReturn(Optional.of(segment));
    }

    private Segment savedSegment() {
        final var saved = ArgumentCaptor.forClass(Segment.class);
        verify(segmentRepository).save(saved.capture());
        return saved.getValue();
    }

    private static Point withId(Point point, Long id) {
        ReflectionTestUtils.setField(point, "id", id);
        return point;
    }
}
