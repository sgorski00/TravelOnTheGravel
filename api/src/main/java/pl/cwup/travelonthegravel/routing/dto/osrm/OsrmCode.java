package pl.cwup.travelonthegravel.routing.dto.osrm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Kody z pola "code" odpowiedzi OSRM (dokumentacja OSRM, sekcja "Responses").
@Getter
@RequiredArgsConstructor
public enum OsrmCode {
    OK("Ok"),
    NO_ROUTE("NoRoute"),
    NO_SEGMENT("NoSegment"),
    INVALID_QUERY("InvalidQuery"),
    INVALID_VALUE("InvalidValue"),
    TOO_BIG("TooBig"),
    UNKNOWN("Unknown");

    @JsonValue
    private final String value;

    // NoRoute: brak drogi między punktami, NoSegment: punkt za daleko od drogi.
    public boolean isNoRoute() {
        return this == NO_ROUTE || this == NO_SEGMENT;
    }

    @JsonCreator
    public static OsrmCode from(String value) {
        return Arrays.stream(values())
                .filter(code -> code.value.equals(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
