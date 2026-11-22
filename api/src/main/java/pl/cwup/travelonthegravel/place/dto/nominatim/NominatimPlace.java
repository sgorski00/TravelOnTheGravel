package pl.cwup.travelonthegravel.place.dto.nominatim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import pl.cwup.travelonthegravel.place.dto.response.PlaceResponse;

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

    public PlaceResponse toPlaceResponse() {
        final var placeName = name == null || name.isBlank() ? displayName.split(",")[0] : name;
        return new PlaceResponse(
                placeName, displayName, Double.parseDouble(lat), Double.parseDouble(lon), countryCode());
    }
}
