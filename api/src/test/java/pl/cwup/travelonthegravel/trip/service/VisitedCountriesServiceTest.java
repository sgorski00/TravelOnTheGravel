package pl.cwup.travelonthegravel.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cwup.travelonthegravel.trip.repository.PointRepository;

@ExtendWith(MockitoExtension.class)
final class VisitedCountriesServiceTest {

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private VisitedCountriesService visitedCountriesService;

    @Test
    void returnsCountryCodesOfUser() {
        when(pointRepository.findVisitedCountryCodes(1L)).thenReturn(List.of("DE", "PL"));

        assertThat(visitedCountriesService.getVisitedCountries(1L).countryCodes())
                .containsExactly("DE", "PL");
    }

    @Test
    void userWithoutPointsHasEmptyList() {
        when(pointRepository.findVisitedCountryCodes(1L)).thenReturn(List.of());

        assertThat(visitedCountriesService.getVisitedCountries(1L).countryCodes())
                .isEmpty();
    }
}
