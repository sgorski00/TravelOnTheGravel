package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.common.exception.BadRequestException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.place.service.GeocodingService;
import pl.cwup.travelonthegravel.trip.dto.request.PointOrderRequest;
import pl.cwup.travelonthegravel.trip.dto.request.PointRequest;
import pl.cwup.travelonthegravel.trip.model.Point;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;

@ExtendWith(MockitoExtension.class)
final class PointServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 10L;

    @Mock
    private TripService tripService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private SegmentService segmentService;

    @InjectMocks
    private PointService pointService;

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");

    @Test
    void getPointsReturnsPointsInOrder() {
        mockOwnTrip();
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID))
                .thenReturn(List.of(point(trip, "Warszawa", 0), point(trip, "Kraków", 1)));

        assertThat(pointService.getPoints(USER_ID, TRIP_ID)).extracting("name").containsExactly("Warszawa", "Kraków");
    }

    @Test
    void getPointsOfOtherUsersTripThrowsNotFound() {
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenThrow(new NotFoundException("Nie znaleziono podróży"));

        assertThatThrownBy(() -> pointService.getPoints(USER_ID, TRIP_ID)).isInstanceOf(NotFoundException.class);
        verify(pointRepository, never()).findAllByTripIdOrderByPosition(any());
    }

    @Test
    void addPointPutsItAtTheEndWithCountryCode() {
        mockOwnTrip();
        when(pointRepository.countByTripId(TRIP_ID)).thenReturn(2);
        when(geocodingService.countryCode(50.06, 19.94)).thenReturn(Optional.of("PL"));
        when(pointRepository.save(any(Point.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = pointService.addPoint(USER_ID, TRIP_ID, new PointRequest("Kraków", 50.06, 19.94));

        assertThat(response.position()).isEqualTo(2);
        assertThat(response.countryCode()).isEqualTo("PL");
        verify(segmentService).syncWithPoints(trip, Set.of());
    }

    @Test
    void addPointWithoutCountryWhenGeocodingFails() {
        mockOwnTrip();
        when(geocodingService.countryCode(anyDouble(), anyDouble()))
                .thenThrow(new ExternalServiceException("Usługa map jest chwilowo niedostępna"));
        when(pointRepository.save(any(Point.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = pointService.addPoint(USER_ID, TRIP_ID, new PointRequest("Kraków", 50.06, 19.94));

        assertThat(response.countryCode()).isNull();
    }

    @Test
    void updatePointWithSameCoordinatesDoesNotGeocodeAgain() {
        final var point = mockOwnPoint(5L, point(trip, "Warszawa", 0));

        final var response = pointService.updatePoint(
                USER_ID, TRIP_ID, 5L, new PointRequest("Stolica", point.getLatitude(), point.getLongitude()));

        assertThat(response.name()).isEqualTo("Stolica");
        assertThat(response.countryCode()).isEqualTo("PL");
        verify(geocodingService, never()).countryCode(anyDouble(), anyDouble());
        verify(segmentService, never()).syncWithPoints(any(), any());
    }

    @Test
    void updatePointWithNewCoordinatesSetsNewCountry() {
        final var point = mockOwnPoint(5L, withId(point(trip, "Warszawa", 0), 5L));
        when(geocodingService.countryCode(52.52, 13.40)).thenReturn(Optional.of("DE"));

        final var response = pointService.updatePoint(USER_ID, TRIP_ID, 5L, new PointRequest("Berlin", 52.52, 13.40));

        assertThat(response.latitude()).isEqualTo(52.52);
        assertThat(response.countryCode()).isEqualTo("DE");
        verify(segmentService).syncWithPoints(point.getTrip(), Set.of(5L));
    }

    @Test
    void updatePointFromOtherTripThrowsNotFound() {
        mockOwnTrip();
        when(pointRepository.findByIdAndTripId(5L, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointService.updatePoint(USER_ID, TRIP_ID, 5L, new PointRequest("X", 1.0, 2.0)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletePointRenumbersRemainingPoints() {
        final var deleted = mockOwnPoint(5L, point(trip, "Kraków", 1));
        final var first = point(trip, "Warszawa", 0);
        final var last = point(trip, "Gdańsk", 2);
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID)).thenReturn(List.of(first, last));

        pointService.deletePoint(USER_ID, TRIP_ID, 5L);

        verify(pointRepository).delete(deleted);
        assertThat(first.getPosition()).isZero();
        assertThat(last.getPosition()).isEqualTo(1);
        verify(segmentService).syncWithPoints(trip, Set.of());
    }

    @Test
    void reorderPointsSetsPositionsFromList() {
        mockOwnTrip();
        final var warsaw = withId(point(trip, "Warszawa", 0), 1L);
        final var krakow = withId(point(trip, "Kraków", 1), 2L);
        final var gdansk = withId(point(trip, "Gdańsk", 2), 3L);
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID)).thenReturn(List.of(warsaw, krakow, gdansk));

        final var response = pointService.reorderPoints(USER_ID, TRIP_ID, new PointOrderRequest(List.of(3L, 1L, 2L)));

        assertThat(response).extracting("name").containsExactly("Gdańsk", "Warszawa", "Kraków");
        assertThat(gdansk.getPosition()).isZero();
        assertThat(warsaw.getPosition()).isEqualTo(1);
        assertThat(krakow.getPosition()).isEqualTo(2);
        verify(segmentService).syncWithPoints(trip, Set.of());
    }

    @Test
    void reorderWithIncompleteListThrows() {
        assertInvalidOrder(List.of(1L, 2L));
    }

    @Test
    void reorderWithDuplicatesThrows() {
        assertInvalidOrder(List.of(1L, 1L, 2L));
    }

    @Test
    void reorderWithForeignPointThrows() {
        assertInvalidOrder(List.of(1L, 2L, 99L));
    }

    private void assertInvalidOrder(List<Long> ids) {
        mockOwnTrip();
        final var warsaw = withId(point(trip, "Warszawa", 0), 1L);
        final var krakow = withId(point(trip, "Kraków", 1), 2L);
        final var gdansk = withId(point(trip, "Gdańsk", 2), 3L);
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID)).thenReturn(List.of(warsaw, krakow, gdansk));

        assertThatThrownBy(() -> pointService.reorderPoints(USER_ID, TRIP_ID, new PointOrderRequest(ids)))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_POINT_ORDER);
        assertThat(warsaw.getPosition()).isZero();
        assertThat(gdansk.getPosition()).isEqualTo(2);
        verify(segmentService, never()).syncWithPoints(any(), any());
    }

    private void mockOwnTrip() {
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenReturn(trip);
    }

    private Point mockOwnPoint(Long id, Point point) {
        mockOwnTrip();
        when(pointRepository.findByIdAndTripId(id, TRIP_ID)).thenReturn(Optional.of(point));
        return point;
    }

    private static Point withId(Point point, Long id) {
        ReflectionTestUtils.setField(point, "id", id);
        return point;
    }
}
