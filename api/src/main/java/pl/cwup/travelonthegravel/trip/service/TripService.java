package pl.cwup.travelonthegravel.trip.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;
import pl.cwup.travelonthegravel.trip.dto.request.TripRequest;
import pl.cwup.travelonthegravel.trip.dto.response.TripResponse;
import pl.cwup.travelonthegravel.trip.dto.response.TripSummaryResponse;
import pl.cwup.travelonthegravel.trip.model.Trip;
import pl.cwup.travelonthegravel.trip.repository.TripRepository;
import pl.cwup.travelonthegravel.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> getTrips(Long userId) {
        return tripRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(TripSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long userId, Long tripId) {
        return TripResponse.from(findOwnTrip(userId, tripId));
    }

    @Transactional
    public TripResponse createTrip(Long userId, TripRequest request) {
        final var trip = new Trip(
                userRepository.getReferenceById(userId),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.description());
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, TripRequest request) {
        final var trip = findOwnTrip(userId, tripId);
        trip.setName(request.name());
        trip.setDates(request.startDate(), request.endDate());
        trip.setDescription(request.description());
        return TripResponse.from(trip);
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        tripRepository.delete(findOwnTrip(userId, tripId));
    }

    private Trip findOwnTrip(Long userId, Long tripId) {
        return tripRepository
                .findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono podróży"));
    }
}
