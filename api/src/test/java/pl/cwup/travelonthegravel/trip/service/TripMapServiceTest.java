package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.point;
import static pl.cwup.travelonthegravel.TestData.segment;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.geojson.GeoJsonLineString;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapFeatureKind;
import pl.cwup.travelonthegravel.trip.model.TransportType;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;
import pl.cwup.travelonthegravel.trip.repository.SegmentRepository;

@ExtendWith(MockitoExtension.class)
final class TripMapServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 10L;

    @Mock
    private TripService tripService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @InjectMocks
    private TripMapService tripMapService;

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");

    @Test
    void tripMapContainsPointsThenSegments() {
        final var warsaw = point(trip, "Warszawa", 0);
        final var krakow = point(trip, "Kraków", 1);
        final var segment = segment(trip, warsaw, krakow, TransportType.PLANE);
        segment.setRoute(1L, GeoJsonLineString.straightLine(warsaw.coordinates(), krakow.coordinates()), null, false);
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenReturn(trip);
        when(pointRepository.findAllByTripIdOrderByPosition(TRIP_ID)).thenReturn(List.of(warsaw, krakow));
        when(segmentRepository.findAllByTripIdOrderByFromPointPosition(TRIP_ID)).thenReturn(List.of(segment));

        final var map = tripMapService.getTripMap(USER_ID, TRIP_ID, RouteDetail.SIMPLIFIED);

        assertThat(map.features())
                .extracting(feature -> feature.properties().kind())
                .containsExactly(MapFeatureKind.POINT, MapFeatureKind.POINT, MapFeatureKind.SEGMENT);
    }

    @Test
    void tripMapOfOtherUsersTripThrowsNotFound() {
        when(tripService.findOwnTrip(USER_ID, TRIP_ID)).thenThrow(new NotFoundException("Nie znaleziono podróży"));

        assertThatThrownBy(() -> tripMapService.getTripMap(USER_ID, TRIP_ID, RouteDetail.SIMPLIFIED))
                .isInstanceOf(NotFoundException.class);
        verify(pointRepository, never()).findAllByTripIdOrderByPosition(any());
    }

    @Test
    void userMapUsesOnlyQueriesForThatUser() {
        when(pointRepository.findAllByTripUserIdOrderByTripIdAscPositionAsc(USER_ID))
                .thenReturn(List.of(point(trip, "Warszawa", 0)));
        when(segmentRepository.findAllByTripUserIdOrderByTripIdAscFromPointPositionAsc(USER_ID))
                .thenReturn(List.of());

        final var map = tripMapService.getUserMap(USER_ID, RouteDetail.FULL);

        assertThat(map.features()).hasSize(1);
        assertThat(map.bbox()).hasSize(4);
    }
}
