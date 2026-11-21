package pl.cwup.travelonthegravel.place.dto.nominatim;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NominatimAddress(@JsonProperty("country_code") String countryCode) {}
