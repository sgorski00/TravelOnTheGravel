package pl.cwup.travelonthegravel.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
final class DevSeedIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devProfileLoadsSampleDataOnce() {
        assertThat(count("select count(*) from databasechangelog where id = '005-dev-seed'"))
                .isEqualTo(1);
        assertThat(count("select count(*) from users where email like '%@example.com'"))
                .isEqualTo(4);
        assertThat(count("select count(*) from trips")).isEqualTo(6);
        assertThat(count("select count(*) from segments")).isEqualTo(count("select count(*) from points") - 6);
    }

    @Test
    void sampleAccountCanLogInAndSeeTripsMapAndStats() throws Exception {
        final var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"anna@example.com\", \"password\": \"haslo1234\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        final var bearer = "Bearer " + JsonPath.read(response, "$.accessToken");

        mockMvc.perform(get("/api/trips").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/users/me/map").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
        mockMvc.perform(get("/api/users/me/stats").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.tripCount").value(2))
                .andExpect(jsonPath("$.countries.length()").value(4));
    }

    @Test
    void blockedSampleAccountCannotLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"dawid@example.com\", \"password\": \"haslo1234\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
