package pl.cwup.travelonthegravel.routing.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

final class CoordinatesTest {

    @Test
    void toLonLatPutsLongitudeFirst() {
        assertThat(new Coordinates(52.2297, 21.0122).toLonLat()).isEqualTo("21.012200,52.229700");
    }

    @Test
    void toLonLatDoesNotUseScientificNotation() {
        assertThat(new Coordinates(51.5, 0.0001).toLonLat()).isEqualTo("0.000100,51.500000");
        assertThat(new Coordinates(51.5, -0.00005).toLonLat()).isEqualTo("-0.000050,51.500000");
    }

    @Test
    void toLonLatUsesDotEvenWithPolishLocale() {
        final var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("pl-PL"));
        try {
            assertThat(new Coordinates(52.2297, 21.0122).toLonLat()).isEqualTo("21.012200,52.229700");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
