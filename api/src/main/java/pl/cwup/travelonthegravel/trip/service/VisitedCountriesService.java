package pl.cwup.travelonthegravel.trip.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cwup.travelonthegravel.trip.dto.response.VisitedCountriesResponse;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;

@Service
@RequiredArgsConstructor
public class VisitedCountriesService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public VisitedCountriesResponse getVisitedCountries(Long userId) {
        return new VisitedCountriesResponse(pointRepository.findVisitedCountryCodes(userId));
    }
}
