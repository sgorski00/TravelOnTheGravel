package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.trip;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.trip.dto.request.TripRequest;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class TripServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 10L;
    private static final LocalDate START = LocalDate.of(2026, 7, 1);

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TripService tripService;

    private final Trip trip = trip(user("jan@example.com", "jan"), "Wakacje");

    @Test
    void getTripsReturnsOwnTripsFromRepository() {
        when(tripRepository.findAllByUserIdOrderByStartDateDesc(USER_ID)).thenReturn(List.of(trip));

        assertThat(tripService.getTrips(USER_ID))
                .singleElement()
                .satisfies(summary -> assertThat(summary.name()).isEqualTo("Wakacje"));
    }

    @Test
    void getTripReturnsOwnTrip() {
        mockOwnTrip();

        assertThat(tripService.getTrip(USER_ID, TRIP_ID).name()).isEqualTo("Wakacje");
    }

    @Test
    void getTripOfOtherUserThrowsNotFound() {
        when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(USER_ID, TRIP_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createTripSavesTripForUser() {
        final var user = user("jan@example.com", "jan");
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = tripService.createTrip(USER_ID, request("Majówka", START, START.plusDays(2), "Opis"));

        assertThat(response.name()).isEqualTo("Majówka");
        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(START.plusDays(2));
        assertThat(response.description()).isEqualTo("Opis");
    }

    @Test
    void updateTripChangesAllFields() {
        mockOwnTrip();

        final var response = tripService.updateTrip(USER_ID, TRIP_ID, request("Nowa", START.plusDays(1), null, null));

        assertThat(response.name()).isEqualTo("Nowa");
        assertThat(response.startDate()).isEqualTo(START.plusDays(1));
        assertThat(response.endDate()).isNull();
        assertThat(response.description()).isNull();
    }

    @Test
    void updateTripOfOtherUserThrowsNotFound() {
        when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.updateTrip(USER_ID, TRIP_ID, request("Nowa", START, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteTripDeletesOwnTrip() {
        mockOwnTrip();

        tripService.deleteTrip(USER_ID, TRIP_ID);

        verify(tripRepository).delete(trip);
    }

    @Test
    void deleteTripOfOtherUserThrowsNotFound() {
        when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.deleteTrip(USER_ID, TRIP_ID)).isInstanceOf(NotFoundException.class);
        verify(tripRepository, never()).delete(any());
    }

    private void mockOwnTrip() {
        when(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID)).thenReturn(Optional.of(trip));
    }

    private static TripRequest request(String name, LocalDate start, LocalDate end, String description) {
        return new TripRequest(name, start, end, description);
    }
}
