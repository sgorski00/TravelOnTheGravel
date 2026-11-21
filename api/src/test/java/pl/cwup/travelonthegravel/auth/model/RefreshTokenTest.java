package pl.cwup.travelonthegravel.auth.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.user.model.User;

final class RefreshTokenTest {

    private final User user = user("jan@example.com", "jan");

    @Test
    void constructorSetsFields() {
        final var expiresAt = Instant.now().plusSeconds(60);

        final var token = new RefreshToken(user, "hash", expiresAt);

        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void isValidBeforeExpiration() {
        final var token = new RefreshToken(user, "hash", Instant.now().plusSeconds(60));

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void isNotValidAfterExpiration() {
        final var token = new RefreshToken(user, "hash", Instant.now().minusSeconds(60));

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isNotValidWhenRevoked() {
        final var token = new RefreshToken(user, "hash", Instant.now().plusSeconds(60));
        token.setRevoked(true);

        assertThat(token.isValid()).isFalse();
    }
}
