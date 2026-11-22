package pl.cwup.travelonthegravel.trip.dto.response;

import java.util.List;

// countryCodes: kody ISO 3166-1 alpha-2, alfabetycznie.
public record VisitedCountriesResponse(List<String> countryCodes) {}
