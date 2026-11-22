package pl.cwup.travelonthegravel.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.cwup.travelonthegravel.TestData.user;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import pl.cwup.travelonthegravel.common.exception.ForbiddenException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.TripSummaryResponse;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;
import pl.cwup.travelonthegravel.trip.service.TripMapService;
import pl.cwup.travelonthegravel.trip.service.TripService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
final class PublicProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripService tripService;

    @Mock
    private TripMapService tripMapService;

    @InjectMocks
    private PublicProfileService publicProfileService;

    @Test
    void searchAsksForPublicUsersSortedByUsername() {
        final var pageable =
                PageRequest.of(0, 20, Sort.by(Sort.Order.asc("username").ignoreCase()));
        when(userRepository.findByIsPublicTrueAndBlockedFalseAndUsernameContainingIgnoreCase("jan", pageable))
                .thenReturn(new PageImpl<>(List.of(publicUser("jan", 1L)), pageable, 1));

        final var result = publicProfileService.search(" jan ", PageRequest.of(0, 20, Sort.by("email")));

        assertThat(result.getContent()).extracting("username").containsExactly("jan");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void profileOfPublicUserHasUsernameAndTrips() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(publicUser("jan", 1L)));
        final var trip = new TripSummaryResponse(5L, "Wakacje", null, null);
        when(tripService.getTrips(1L)).thenReturn(List.of(trip));

        final var profile = publicProfileService.getProfile(1L);

        assertThat(profile.id()).isEqualTo(1L);
        assertThat(profile.username()).isEqualTo("jan");
        assertThat(profile.trips()).containsExactly(trip);
    }

    @Test
    void privateProfileThrowsProfilePrivate() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user("ola@example.com", "ola")));

        assertThatThrownBy(() -> publicProfileService.getProfile(2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PROFILE_PRIVATE);
        assertThatThrownBy(() -> publicProfileService.getMap(2L, RouteDetail.SIMPLIFIED))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> publicProfileService.getTrip(2L, 5L, RouteDetail.SIMPLIFIED))
                .isInstanceOf(ForbiddenException.class);
        verify(tripService, never()).getTrips(any());
        verify(tripMapService, never()).getUserMap(any(), any());
    }

    @Test
    void unknownUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicProfileService.getProfile(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void mapAndTripAreLoadedForProfileOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(publicUser("jan", 1L)));
        final var map = MapResponse.of(List.of());
        when(tripMapService.getUserMap(1L, RouteDetail.FULL)).thenReturn(map);
        when(tripMapService.getTripMap(1L, 5L, RouteDetail.SIMPLIFIED)).thenReturn(map);

        assertThat(publicProfileService.getMap(1L, RouteDetail.FULL)).isSameAs(map);
        assertThat(publicProfileService.getTrip(1L, 5L, RouteDetail.SIMPLIFIED).map())
                .isSameAs(map);
        verify(tripService).getTrip(1L, 5L);
    }

    private static User publicUser(String username, Long id) {
        final var user = user(username + "@example.com", username);
        user.setPublic(true);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void blockedUserIsNotFound() {
        final var blocked = publicUser("jan", 1L);
        blocked.setBlocked(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> publicProfileService.getProfile(1L)).isInstanceOf(NotFoundException.class);
    }
}
