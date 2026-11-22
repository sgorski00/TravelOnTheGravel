package pl.cwup.travelonthegravel.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

final class AdminPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

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

    @Test
    void invalidEmailStopsApplicationStartup() {
        contextRunner
                .withPropertyValues("app.admin.email=admin", "app.admin.password=tajnehaslo")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("ADMIN_EMAIL musi być poprawnym adresem e-mail"));
    }

    @Test
    void validOrEmptyEmailStartsApplication() {
        contextRunner
                .withPropertyValues("app.admin.email=admin@example.com", "app.admin.password=tajnehaslo")
                .run(context -> assertThat(context).hasNotFailed());
        contextRunner
                .withPropertyValues("app.admin.email=")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @EnableConfigurationProperties(AdminProperties.class)
    static class TestConfig {}
}
