package pl.cwup.travelonthegravel.place.dto.nominatim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
}
