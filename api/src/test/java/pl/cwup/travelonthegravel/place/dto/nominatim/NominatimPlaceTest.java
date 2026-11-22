package pl.cwup.travelonthegravel.place.dto.nominatim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;

final class NominatimPlaceTest {

    @Test
    void countryCodeIsUppercase() {
        assertThat(place(new NominatimAddress("pl")).countryCode()).isEqualTo("PL");
    }

    @Test
    void countryCodeIsNullWithoutAddressOrCode() {
        assertThat(place(null).countryCode()).isNull();
        assertThat(place(new NominatimAddress(null)).countryCode()).isNull();
    }

    private static NominatimPlace place(NominatimAddress address) {
        return new NominatimPlace("1", "2", "Miejsce", "Miejsce, Kraj", address);
    }

    @Test
    void toPlaceResponseMapsAllFields() {
        final var place = new NominatimPlace(
                "50.0619474", "19.9368564", "Kraków", "Kraków, małopolskie, Polska", new NominatimAddress("pl"));

        assertThat(place.toPlaceResponse())
                .isEqualTo(new PlaceResponse("Kraków", "Kraków, małopolskie, Polska", 50.0619474, 19.9368564, "PL"));
    }

    @Test
    void toPlaceResponseUsesFirstPartOfDisplayNameWhenNameIsMissing() {
        assertThat(new NominatimPlace("1", "2", null, "Rynek Główny, Kraków", null)
                        .toPlaceResponse()
                        .name())
                .isEqualTo("Rynek Główny");
        assertThat(new NominatimPlace("1", "2", " ", "Rynek Główny, Kraków", null)
                        .toPlaceResponse()
                        .name())
                .isEqualTo("Rynek Główny");
    }
}
