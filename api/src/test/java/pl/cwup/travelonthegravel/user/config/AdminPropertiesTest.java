package pl.cwup.travelonthegravel.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class AdminPropertiesTest {

    @Test
    void configuredWhenEmailAndPasswordAreSet() {
        assertThat(new AdminProperties("admin@example.com", "admin", "tajnehaslo").isConfigured())
                .isTrue();
    }

    @Test
    void notConfiguredWhenEmailOrPasswordIsMissing() {
        assertThat(new AdminProperties("", "admin", "tajnehaslo").isConfigured())
                .isFalse();
        assertThat(new AdminProperties("admin@example.com", "admin", " ").isConfigured())
                .isFalse();
        assertThat(new AdminProperties(null, "admin", null).isConfigured()).isFalse();
    }
}
