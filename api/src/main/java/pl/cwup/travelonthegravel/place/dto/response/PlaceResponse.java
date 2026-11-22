package pl.cwup.travelonthegravel.place.dto.response;

import pl.cwup.travelonthegravel.place.dto.nominatim.NominatimPlace;

public record PlaceResponse(String name, String displayName, double latitude, double longitude, String countryCode) {

    public static PlaceResponse from(NominatimPlace place) {
        final var name = place.name() == null || place.name().isBlank()
                ? place.displayName().split(",")[0]
                : place.name();
        return new PlaceResponse(
                name,
                place.displayName(),
                Double.parseDouble(place.lat()),
                Double.parseDouble(place.lon()),
                place.countryCode());
    }
}
