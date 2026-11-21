package pl.cwup.travelonthegravel.auth.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.cwup.travelonthegravel.TestData.user;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.user.model.User;

final class PasswordResetTokenTest {

    private final User user = user("jan@example.com", "jan");

    @Test
    void constructorSetsFields() {
        final var expiresAt = Instant.now().plusSeconds(60);

        final var token = new PasswordResetToken(user, "hash", expiresAt);

        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void isValidBeforeExpiration() {
        PasswordResetToken token =
                new PasswordResetToken(user, "hash", Instant.now().plusSeconds(60));

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void isNotValidAfterExpiration() {
        PasswordResetToken token =
                new PasswordResetToken(user, "hash", Instant.now().minusSeconds(60));

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isNotValidWhenUsed() {
        PasswordResetToken token =
                new PasswordResetToken(user, "hash", Instant.now().plusSeconds(60));
        token.setUsed(true);

        assertThat(token.isValid()).isFalse();
    }
}
