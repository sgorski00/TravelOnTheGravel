package pl.cwup.travelonthegravel.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class DefaultProfileWithoutSeedIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sampleDataIsNotLoadedWithoutDevProfile() {
        final var seedRuns = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = '005-dev-seed'", Long.class);
        final var schemaRuns = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = '001-init-schema'", Long.class);

        assertThat(seedRuns).isZero();
        assertThat(schemaRuns).isEqualTo(1);
    }
}
