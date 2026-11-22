package pl.cwup.travelonthegravel.place.dto.nominatim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

public record NominatimPlace(
        String lat,
        String lon,
        String name,
        @JsonProperty("display_name") String displayName,
        NominatimAddress address) {

    public String countryCode() {
        return address == null || address.countryCode() == null
                ? null
                : address.countryCode().toUpperCase(Locale.ROOT);
    }
}
