package pl.cwup.travelonthegravel.routing.dto.osrm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

final class OsrmCodeTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void fromReadsOsrmValues() {
        assertThat(OsrmCode.from("Ok")).isEqualTo(OsrmCode.OK);
        assertThat(OsrmCode.from("NoRoute")).isEqualTo(OsrmCode.NO_ROUTE);
        assertThat(OsrmCode.from("NoSegment")).isEqualTo(OsrmCode.NO_SEGMENT);
    }

    @Test
    void unknownCodeIsUnknownNotError() {
        assertThat(OsrmCode.from("SomethingNew")).isEqualTo(OsrmCode.UNKNOWN);
        assertThat(OsrmCode.from(null)).isEqualTo(OsrmCode.UNKNOWN);
    }

    @Test
    void onlyNoRouteAndNoSegmentMeanNoRoute() {
        assertThat(OsrmCode.NO_ROUTE.isNoRoute()).isTrue();
        assertThat(OsrmCode.NO_SEGMENT.isNoRoute()).isTrue();
        assertThat(OsrmCode.OK.isNoRoute()).isFalse();
        assertThat(OsrmCode.INVALID_QUERY.isNoRoute()).isFalse();
        assertThat(OsrmCode.UNKNOWN.isNoRoute()).isFalse();
    }

    @Test
    void responseCodeIsReadFromJson() {
        final var response = jsonMapper.readValue(
                "{\"code\": \"NoRoute\", \"message\": \"Impossible route between points\"}", OsrmRouteResponse.class);

        assertThat(response.code()).isEqualTo(OsrmCode.NO_ROUTE);
    }
}
