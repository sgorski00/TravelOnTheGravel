package pl.cwup.travelonthegravel.email.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import pl.cwup.travelonthegravel.TestcontainersConfiguration;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
final class EmailEventListenerIT {

    private static final int SMTP_PORT = 1025;
    private static final int API_PORT = 8025;

    private static final GenericContainer<?> MAILPIT =
            new GenericContainer<>("axllent/mailpit:v1.34").withExposedPorts(SMTP_PORT, API_PORT);

    static {
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(SMTP_PORT));
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final RestClient mailpit =
            RestClient.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(API_PORT));

    @Test
    void passwordResetEventSendsEmailWithLinkAndToken() {
        eventPublisher.publishEvent(new PasswordResetRequestedEvent("jan@example.com", "abc123"));

        final var expectedLink = "http://localhost:3000/reset-password?token=abc123";
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            final var messages =
                    mailpit.get().uri("/api/v1/messages").retrieve().body(String.class);
            assertThat(JsonPath.<Integer>read(messages, "$.total")).isEqualTo(1);

            final String id = JsonPath.read(messages, "$.messages[0].ID");
            final var message =
                    mailpit.get().uri("/api/v1/message/" + id).retrieve().body(String.class);
            assertThat(JsonPath.<String>read(message, "$.To[0].Address")).isEqualTo("jan@example.com");
            assertThat(JsonPath.<String>read(message, "$.Subject")).isEqualTo("Reset hasła - TravelOnTheGravel");
            assertThat(JsonPath.<String>read(message, "$.Text")).contains(expectedLink);
            assertThat(JsonPath.<String>read(message, "$.HTML")).contains(expectedLink);
            assertThat(JsonPath.<String>read(message, "$.Text"))
                    .contains("Otrzymaliśmy prośbę o ustawienie nowego hasła")
                    .doesNotContain("??");
            assertThat(JsonPath.<String>read(message, "$.HTML")).doesNotContain("??");
        });
    }
}
