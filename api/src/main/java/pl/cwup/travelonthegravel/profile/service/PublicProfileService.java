package pl.cwup.travelonthegravel.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.common.exception.ForbiddenException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.common.model.ErrorCode;
import pl.cwup.travelonthegravel.profile.dto.response.PublicProfileResponse;
import pl.cwup.travelonthegravel.profile.dto.response.PublicTripResponse;
import pl.cwup.travelonthegravel.profile.dto.response.PublicUserResponse;
import pl.cwup.travelonthegravel.routing.model.RouteDetail;
import pl.cwup.travelonthegravel.trip.dto.response.map.MapResponse;
import pl.cwup.travelonthegravel.trip.service.TripMapService;
import pl.cwup.travelonthegravel.trip.service.TripService;
import pl.cwup.travelonthegravel.user.model.User;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final UserRepository userRepository;
    private final TripService tripService;
    private final TripMapService tripMapService;

    @Transactional(readOnly = true)
    public Page<PublicUserResponse> search(String query, Pageable pageable) {
        final var sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("username").ignoreCase()));
        return userRepository
                .findByIsPublicTrueAndUsernameContainingIgnoreCase(query.trim(), sorted)
                .map(PublicUserResponse::from);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getProfile(Long userId) {
        final var user = findPublicUser(userId);
        return new PublicProfileResponse(user.getId(), user.getUsername(), tripService.getTrips(user.getId()));
    }

    @Transactional(readOnly = true)
    public MapResponse getMap(Long userId, RouteDetail detail) {
        return tripMapService.getUserMap(findPublicUser(userId).getId(), detail);
    }

    @Transactional(readOnly = true)
    public PublicTripResponse getTrip(Long userId, Long tripId, RouteDetail detail) {
        final var owner = findPublicUser(userId);
        return new PublicTripResponse(
                tripService.getTrip(owner.getId(), tripId), tripMapService.getTripMap(owner.getId(), tripId, detail));
    }

    private User findPublicUser(Long userId) {
        final var user =
                userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika"));
        if (!user.isPublic()) {
            throw new ForbiddenException(ErrorCode.PROFILE_PRIVATE, "Profil użytkownika jest prywatny");
        }
        return user;
    }
}
