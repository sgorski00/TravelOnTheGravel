package pl.cwup.travelonthegravel.place.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimAddress;
import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;

final class PlaceResponseTest {

    @Test
    void fromMapsNominatimPlace() {
        final var place = new NominatimPlace(
                "50.0619474", "19.9368564", "Kraków", "Kraków, małopolskie, Polska", new NominatimAddress("pl"));

        assertThat(PlaceResponse.from(place))
                .isEqualTo(new PlaceResponse("Kraków", "Kraków, małopolskie, Polska", 50.0619474, 19.9368564, "PL"));
    }

    @Test
    void fromUsesFirstPartOfDisplayNameWhenNameIsMissing() {
        assertThat(PlaceResponse.from(new NominatimPlace("1", "2", null, "Rynek Główny, Kraków", null))
                        .name())
                .isEqualTo("Rynek Główny");
        assertThat(PlaceResponse.from(new NominatimPlace("1", "2", " ", "Rynek Główny, Kraków", null))
                        .name())
                .isEqualTo("Rynek Główny");
    }
}
